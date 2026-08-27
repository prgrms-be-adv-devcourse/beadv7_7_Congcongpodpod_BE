import { router } from 'expo-router';
import { type PropsWithChildren, useEffect, useRef } from 'react';
import { AppState } from 'react-native';

import { showDishReport, showInAppNotification } from '@/lib/app-overlay';
import { getDishReportSnapshot } from '@/lib/dish-report';
import { notifyMemberBenefitsChanged } from '@/lib/member-benefit-events';
import { notifyOrderStateChanged } from '@/lib/order-events';
import { canShowNotification, DEFAULT_NOTIFICATION_PREFERENCES, loadNotificationPreferences, subscribeNotificationPreferences } from '@/lib/notification-preferences';
import { connectNotificationStream, getNotifications, notificationRoute, type ServerNotification } from '@/lib/notifications';
import { refreshAccessToken } from '@/lib/api';
import { useAuth } from '@/providers/auth-provider';

const DEMO_ENABLED = process.env.EXPO_PUBLIC_NOTIFICATION_DEMO === 'true';
const DEMO_INTERVAL_MS = 30_000;
const RECONNECT_BASE_MS = 1_000;
const RECONNECT_MAX_MS = 30_000;
const RECONNECT_JITTER = 0.25;
const RECONCILE_INTERVAL_MS = 12_000;
const demoNotifications: ServerNotification[] = [
  { id: -1, type: 'ORDER_ACCEPTED', title: '주문이 접수됐어요', body: '매장에서 주문을 확인하고 음식을 준비하고 있어요.', linkTarget: 'ORDER', readYn: false, createdAt: '' },
  { id: -2, type: 'PICKUP_READY', title: '픽업 준비가 완료됐어요', body: '주문 내역에서 픽업 코드를 확인해주세요.', linkTarget: 'ORDER', readYn: false, createdAt: '' },
  { id: -3, type: 'PICKED_UP', title: '픽업이 완료됐어요', body: '맛있는 한 끼를 구조했어요. 이용해주셔서 감사합니다.', linkTarget: 'ORDER', readYn: false, createdAt: '' },
];
const knownNotificationTypes = new Set(['ORDER_CREATED', 'ORDER_ACCEPTED', 'PICKUP_READY', 'PICKUP_STARTED', 'PICKUP_DEADLINE_SOON', 'PICKED_UP', 'ORDER_NO_SHOW', 'ORDER_CANCELLED', 'ORDER_REJECTED', 'POINT_EARNED', 'DISH_REPORT_COMPLETED']);
const orderStateTypes = new Set(['ORDER_CREATED', 'ORDER_ACCEPTED', 'PICKUP_READY', 'PICKUP_STARTED', 'PICKUP_DEADLINE_SOON', 'PICKED_UP', 'ORDER_NO_SHOW', 'ORDER_CANCELLED', 'ORDER_REJECTED']);

function readablePayload(data?: string | null) {
  if (!data) return undefined;
  try {
    const parsed = JSON.parse(data) as unknown;
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) return String(parsed);
    return Object.entries(parsed as Record<string, unknown>).map(([key, value]) => `${key}: ${String(value)}`).join(' · ');
  } catch {
    return data;
  }
}

function handleNotificationEvent(notification: ServerNotification) {
  const normalizedType = notification.type?.toUpperCase() ?? '';
  if (orderStateTypes.has(normalizedType)) notifyOrderStateChanged({ type: normalizedType, orderId: notification.linkId });
  if (normalizedType === 'POINT_EARNED') notifyMemberBenefitsChanged();
  if (normalizedType === 'DISH_REPORT_COMPLETED') notifyMemberBenefitsChanged();
}

function present(notification: ServerNotification) {
  const normalizedType = notification.type?.toUpperCase() ?? '';
  if (normalizedType === 'DISH_REPORT_COMPLETED') {
    void getDishReportSnapshot().then(showDishReport).catch(() => showDishReport());
    return;
  }
  const unknownType = !knownNotificationTypes.has(normalizedType);
  const payload = readablePayload(notification.data);
  const title = notification.title?.trim() || '새 알림이 도착했어요';
  const message = unknownType
    ? payload || notification.body?.trim() || '새로운 소식을 확인해주세요.'
    : notification.body?.trim() || payload || '알림 내용을 확인해주세요.';
  showInAppNotification(title, message, () => router.push(notificationRoute(notification) as never), notification.type, unknownType ? payload : undefined);
}

export function notificationReconnectDelay(attempt: number, random = Math.random) {
  const exponential = Math.min(RECONNECT_MAX_MS, RECONNECT_BASE_MS * 2 ** Math.max(0, attempt));
  const jitter = 1 - RECONNECT_JITTER + random() * RECONNECT_JITTER * 2;
  return Math.min(RECONNECT_MAX_MS, Math.round(exponential * jitter));
}

export function NotificationProvider({ children }: PropsWithChildren) {
  const { member } = useAuth();
  const reconnectAttempt = useRef(0);
  const preferences = useRef(DEFAULT_NOTIFICATION_PREFERENCES);

  useEffect(() => {
    const unsubscribe = subscribeNotificationPreferences(next => { preferences.current = next; });
    void loadNotificationPreferences();
    return unsubscribe;
  }, []);

  useEffect(() => {
    if (!member) return;
    let disposed = false;
    let disconnect: (() => void) | undefined;
    let reconnectTimer: ReturnType<typeof setTimeout> | undefined;
    let reconcileTimer: ReturnType<typeof setInterval> | undefined;
    let refreshingToken = false;
    let reconciling = false;
    let baselineLoaded = false;
    const receivedIds = new Set<number>();

    const deliver = (notification: ServerNotification) => {
      if (receivedIds.has(notification.id)) return;
      receivedIds.add(notification.id);
      handleNotificationEvent(notification);
      if (canShowNotification(preferences.current, notification.type)) present(notification);
    };

    const reconcile = async () => {
      if (disposed || reconciling) return;
      reconciling = true;
      try {
        const page = await getNotifications(0, 30);
        const notifications = [...page.items].reverse();
        if (!baselineLoaded) {
          notifications.forEach(notification => receivedIds.add(notification.id));
          baselineLoaded = true;
          return;
        }
        notifications.forEach(deliver);
      } catch {
        // SSE 재연결이 별도로 동작한다. 조회 실패가 앱 사용을 막지 않는다.
      } finally {
        reconciling = false;
      }
    };

    const scheduleReconnect = () => {
      if (disposed || reconnectTimer) return;
      const delay = notificationReconnectDelay(reconnectAttempt.current);
      reconnectAttempt.current += 1;
      reconnectTimer = setTimeout(() => {
        reconnectTimer = undefined;
        connect();
      }, delay);
    };

    const connect = () => {
      if (disposed) return;
      disconnect?.();
      disconnect = connectNotificationStream(notification => {
        deliver(notification);
      }, ({ status }) => {
        if (disposed) return;
        if (status === 401 && !refreshingToken) {
          refreshingToken = true;
          void refreshAccessToken()
            .then(() => { if (!disposed) connect(); })
            .catch(scheduleReconnect)
            .finally(() => { refreshingToken = false; });
          return;
        }
        scheduleReconnect();
      }, () => {
        reconnectAttempt.current = 0;
        if (reconnectTimer) clearTimeout(reconnectTimer);
        reconnectTimer = undefined;
      });
    };

    void reconcile().finally(connect);
    reconcileTimer = setInterval(() => {
      if (AppState.currentState === 'active') void reconcile();
    }, RECONCILE_INTERVAL_MS);
    return () => {
      disposed = true;
      reconnectAttempt.current = 0;
      if (reconnectTimer) clearTimeout(reconnectTimer);
      if (reconcileTimer) clearInterval(reconcileTimer);
      disconnect?.();
    };
  }, [member]);

  useEffect(() => {
    if (!member || !DEMO_ENABLED) return;
    let index = 0;
    const timer = setInterval(() => {
      const notification = demoNotifications[index % demoNotifications.length];
      if (canShowNotification(preferences.current, notification.type)) present(notification);
      index += 1;
    }, DEMO_INTERVAL_MS);
    return () => clearInterval(timer);
  }, [member]);

  return children;
}
