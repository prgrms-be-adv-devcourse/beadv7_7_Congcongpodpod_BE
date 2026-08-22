import { router } from 'expo-router';
import { type PropsWithChildren, useEffect, useRef } from 'react';

import { showInAppNotification } from '@/lib/app-overlay';
import { connectNotificationStream, notificationRoute, type ServerNotification } from '@/lib/notifications';
import { useAuth } from '@/providers/auth-provider';

const DEMO_ENABLED = process.env.EXPO_PUBLIC_NOTIFICATION_DEMO !== 'false';
const DEMO_INTERVAL_MS = 30_000;
const RECONNECT_DELAYS = [2_000, 5_000, 15_000, 30_000] as const;
const demoNotifications: ServerNotification[] = [
  { id: -1, type: 'ORDER_ACCEPTED', title: '주문이 접수됐어요', body: '매장에서 주문을 확인하고 음식을 준비하고 있어요.', linkTarget: 'ORDER', readYn: false, createdAt: '' },
  { id: -2, type: 'PICKUP_READY', title: '픽업 준비가 완료됐어요', body: '주문 내역에서 픽업 코드를 확인해주세요.', linkTarget: 'ORDER', readYn: false, createdAt: '' },
  { id: -3, type: 'PICKED_UP', title: '픽업이 완료됐어요', body: '맛있는 한 끼를 구조했어요. 이용해주셔서 감사합니다.', linkTarget: 'ORDER', readYn: false, createdAt: '' },
];
const knownNotificationTypes = new Set(['ORDER_ACCEPTED', 'PICKUP_READY', 'PICKED_UP', 'ORDER_CANCELLED', 'ORDER_REJECTED']);

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

function present(notification: ServerNotification) {
  const unknownType = !knownNotificationTypes.has(notification.type?.toUpperCase() ?? '');
  const payload = readablePayload(notification.data);
  const title = notification.title?.trim() || '새 알림이 도착했어요';
  const message = unknownType
    ? payload || notification.body?.trim() || '새로운 소식을 확인해주세요.'
    : notification.body?.trim() || payload || '알림 내용을 확인해주세요.';
  showInAppNotification(title, message, () => router.push(notificationRoute(notification) as never), notification.type, unknownType ? payload : undefined);
}

export function NotificationProvider({ children }: PropsWithChildren) {
  const { member } = useAuth();
  const reconnectAttempt = useRef(0);

  useEffect(() => {
    if (!member) return;
    let disposed = false;
    let disconnect: (() => void) | undefined;
    let reconnectTimer: ReturnType<typeof setTimeout> | undefined;

    const connect = () => {
      disconnect?.();
      disconnect = connectNotificationStream(notification => {
        reconnectAttempt.current = 0;
        present(notification);
      }, () => {
        if (disposed) return;
        const delay = RECONNECT_DELAYS[Math.min(reconnectAttempt.current, RECONNECT_DELAYS.length - 1)];
        reconnectAttempt.current += 1;
        reconnectTimer = setTimeout(connect, delay);
      });
    };

    connect();
    return () => {
      disposed = true;
      reconnectAttempt.current = 0;
      if (reconnectTimer) clearTimeout(reconnectTimer);
      disconnect?.();
    };
  }, [member]);

  useEffect(() => {
    if (!member || !DEMO_ENABLED) return;
    let index = 0;
    const timer = setInterval(() => {
      present(demoNotifications[index % demoNotifications.length]);
      index += 1;
    }, DEMO_INTERVAL_MS);
    return () => clearInterval(timer);
  }, [member]);

  return children;
}
