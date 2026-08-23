import { RoundedIcon as Ionicons } from '@/components/rounded-icon';
import type { PropsWithChildren } from 'react';
import { useEffect, useRef, useState } from 'react';
import { Animated, Easing, Modal, Pressable, StyleSheet, Text, useWindowDimensions, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { LoadingState } from '@/components/loading-state';
import { colors, fonts, radius, shadow } from '@/constants/theme';
import { type AppAlertRequest, type AppNotificationRequest, subscribeAppAlerts, subscribeGlobalLoading, subscribeInAppNotifications } from '@/lib/app-overlay';

const MIN_LOADING_VISIBLE_MS = 160;

function notificationVisual(type?: string) {
  if (type === 'ORDER_ACCEPTED') return { icon: 'receipt-outline' as const, label: '주문 접수' };
  if (type === 'PICKUP_READY') return { icon: 'bag-check-outline' as const, label: '픽업 준비 완료' };
  if (type === 'PICKED_UP') return { icon: 'checkmark-circle-outline' as const, label: '픽업 완료' };
  if (type === 'ORDER_CANCELLED') return { icon: 'close-circle-outline' as const, label: '주문 취소' };
  if (type === 'ORDER_REJECTED') return { icon: 'alert-circle-outline' as const, label: '주문 거절' };
  return { icon: 'notifications-outline' as const, label: '새 알림' };
}

function NotificationToast({ notification, onDismiss }: { notification: AppNotificationRequest; onDismiss: (id: number, onDismissed?: () => void) => void }) {
  const motion = useRef(new Animated.Value(0)).current;
  const visual = notificationVisual(notification.type);

  useEffect(() => {
    Animated.timing(motion, { toValue: 1, duration: 260, easing: Easing.bezier(0.22, 1, 0.36, 1), useNativeDriver: true }).start();
  }, [motion]);

  const dismiss = (after?: () => void) => {
    Animated.timing(motion, { toValue: 0, duration: 160, easing: Easing.out(Easing.cubic), useNativeDriver: true }).start(() => onDismiss(notification.id, after));
  };

  return <Animated.View style={[styles.notificationAnimated, { opacity: motion, transform: [{ translateY: motion.interpolate({ inputRange: [0, 1], outputRange: [-16, 0] }) }, { scale: motion.interpolate({ inputRange: [0, 1], outputRange: [0.98, 1] }) }] }]}>
    <Pressable accessibilityRole="button" accessibilityLabel={`${notification.title}. ${notification.message}. 자세히 보기`} onPress={notification.onPress} style={({ pressed }) => [styles.notificationCard, pressed && styles.notificationPressed]}>
      <View style={styles.notificationIcon}><Ionicons name={visual.icon} size={22} color={colors.green700}/><View style={styles.notificationUnread}/></View>
      <View style={styles.notificationCopy}>
        <View style={styles.notificationMeta}><Text style={styles.notificationLabel}>{visual.label}</Text><Text style={styles.notificationTime}>방금</Text></View>
        <Text numberOfLines={1} style={styles.notificationTitle}>{notification.title}</Text>
        <Text numberOfLines={3} style={styles.notificationMessage}>{notification.message}</Text>
      </View>
      <View style={styles.notificationActions}>
        <Pressable accessibilityLabel="알림 닫기" hitSlop={8} onPress={(event) => { event.stopPropagation(); dismiss(); }} style={styles.notificationClose}><Ionicons name="close" size={18} color={colors.ink500}/></Pressable>
        {notification.onPress ? <View style={styles.notificationOpen}><Text style={styles.notificationOpenText}>보기</Text><Ionicons name="chevron-forward" size={14} color={colors.ink900}/></View> : null}
      </View>
    </Pressable>
  </Animated.View>;
}

export function AppOverlayProvider({ children }: PropsWithChildren) {
  const insets = useSafeAreaInsets();
  const { height } = useWindowDimensions();
  const [alert, setAlert] = useState<AppAlertRequest>();
  const [loadingCount, setLoadingCount] = useState(0);
  const [showLoading, setShowLoading] = useState(false);
  const [notifications, setNotifications] = useState<AppNotificationRequest[]>([]);
  const loadingVisible = useRef(false);
  const loadingShownAt = useRef(0);
  const hideLoadingTimer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);
  const maxVisibleNotifications = Math.max(1, Math.min(4, Math.floor((height - insets.top - insets.bottom - 24) / 116)));
  const maxVisibleRef = useRef(maxVisibleNotifications);

  useEffect(() => subscribeAppAlerts(setAlert), []);
  useEffect(() => subscribeGlobalLoading(setLoadingCount), []);
  useEffect(() => {
    maxVisibleRef.current = maxVisibleNotifications;
    setNotifications(current => current.slice(-maxVisibleNotifications));
  }, [maxVisibleNotifications]);
  useEffect(() => subscribeInAppNotifications(request => {
    setNotifications(current => [...current, request].slice(-maxVisibleRef.current));
  }), []);
  useEffect(() => {
    if (hideLoadingTimer.current) clearTimeout(hideLoadingTimer.current);
    if (loadingCount > 0) {
      if (!loadingVisible.current) {
        loadingVisible.current = true;
        loadingShownAt.current = Date.now();
        setShowLoading(true);
      }
      return;
    }
    if (!loadingVisible.current) return;
    const remaining = Math.max(0, MIN_LOADING_VISIBLE_MS - (Date.now() - loadingShownAt.current));
    hideLoadingTimer.current = setTimeout(() => {
      loadingVisible.current = false;
      setShowLoading(false);
    }, remaining);
  }, [loadingCount]);
  useEffect(() => () => { if (hideLoadingTimer.current) clearTimeout(hideLoadingTimer.current); }, []);

  const dismiss = () => {
    const cancel = alert?.buttons.find(button => button.style === 'cancel');
    setAlert(undefined);
    cancel?.onPress?.();
  };

  const dismissNotification = (id: number, onDismissed?: () => void) => {
    setNotifications(current => current.filter(item => item.id !== id));
    onDismissed?.();
  };

  return <>
    {children}
    {notifications.length ? <View pointerEvents="box-none" style={[styles.notificationLayer, { top: Math.max(12, insets.top + 8) }]}>{notifications.map(notification => <NotificationToast key={notification.id} notification={notification} onDismiss={dismissNotification}/>)}</View> : null}
    <Modal animationType="fade" onRequestClose={dismiss} presentationStyle="overFullScreen" transparent visible={Boolean(alert)}>
      <View style={[styles.root, { paddingBottom: Math.max(20, insets.bottom) }]}>
        <Pressable accessibilityLabel="알림 닫기" onPress={dismiss} style={styles.scrim}/>
        {alert ? <View accessibilityRole="alert" accessibilityViewIsModal style={styles.card}>
          <View style={[styles.icon, alert.buttons.some(button => button.style === 'destructive') && styles.iconDanger]}><Ionicons name={alert.buttons.some(button => button.style === 'destructive') ? 'alert-circle-outline' : 'notifications-outline'} size={21} color={alert.buttons.some(button => button.style === 'destructive') ? colors.danger700 : colors.green700}/></View>
          <Text style={styles.title}>{alert.title}</Text>
          {alert.message ? <Text style={styles.message}>{alert.message}</Text> : null}
          <View style={styles.actions}>{alert.buttons.map((button, index) => <Pressable accessibilityRole="button" key={`${alert.id}-${index}`} onPress={() => { setAlert(undefined); button.onPress?.(); }} style={({ pressed }) => [styles.action, button.style === 'cancel' && styles.cancel, button.style === 'destructive' && styles.danger, pressed && styles.pressed]}><Text style={[styles.actionText, button.style === 'cancel' && styles.cancelText]}>{button.text ?? '확인'}</Text></Pressable>)}</View>
        </View> : null}
      </View>
    </Modal>
    {showLoading ? <View accessibilityLabel="처리 중" accessibilityRole="progressbar" pointerEvents="none" style={styles.loadingRoot}><View style={styles.loadingCard}><LoadingState compact inline label="잠시만 기다려주세요"/></View></View> : null}
  </>;
}

const styles = StyleSheet.create({
  root: { flex: 1, alignItems: 'center', justifyContent: 'center', paddingHorizontal: 24 },
  scrim: { ...StyleSheet.absoluteFillObject, backgroundColor: 'rgba(15,20,17,0.48)' },
  card: { width: '100%', maxWidth: 360, paddingHorizontal: 20, paddingTop: 24, paddingBottom: 18, alignItems: 'center', borderRadius: radius.card, backgroundColor: colors.white, ...shadow.float },
  icon: { width: 44, height: 44, alignItems: 'center', justifyContent: 'center', borderRadius: 22, backgroundColor: colors.green50 },
  iconDanger: { backgroundColor: colors.danger50 },
  title: { marginTop: 15, color: colors.ink900, fontFamily: fonts.body, fontSize: 20, lineHeight: 27, fontWeight: '900', letterSpacing: -0.5, textAlign: 'center' },
  message: { marginTop: 7, color: colors.ink700, fontFamily: fonts.body, fontSize: 13, lineHeight: 20, textAlign: 'center' },
  actions: { width: '100%', marginTop: 22, flexDirection: 'row', gap: 8 },
  action: { minHeight: 50, flex: 1, alignItems: 'center', justifyContent: 'center', borderRadius: radius.input, backgroundColor: colors.green500 },
  cancel: { borderWidth: 1, borderColor: colors.lineStrong, backgroundColor: colors.white },
  danger: { backgroundColor: colors.danger700 },
  actionText: { color: colors.white, fontFamily: fonts.body, fontSize: 14, fontWeight: '900' },
  cancelText: { color: colors.ink900 },
  pressed: { opacity: 0.72, transform: [{ scale: 0.985 }] },
  loadingRoot: { ...StyleSheet.absoluteFillObject, zIndex: 2000, alignItems: 'center', justifyContent: 'center', paddingHorizontal: 24, backgroundColor: 'rgba(15,20,17,0.26)' },
  loadingCard: { width: 148, minHeight: 104, alignItems: 'center', justifyContent: 'center', borderRadius: radius.card, backgroundColor: colors.white, ...shadow.float },
  notificationLayer: { position: 'absolute', left: 12, right: 12, zIndex: 1000, alignItems: 'center', gap: 8 },
  notificationAnimated: { width: '100%', maxWidth: 440 },
  notificationCard: { width: '100%', minHeight: 104, paddingLeft: 14, paddingRight: 10, paddingVertical: 13, flexDirection: 'row', alignItems: 'center', gap: 12, overflow: 'hidden', borderRadius: radius.card, backgroundColor: colors.white, borderWidth: 1, borderColor: colors.lineStrong, ...shadow.float },
  notificationIcon: { width: 46, height: 46, alignItems: 'center', justifyContent: 'center', borderRadius: 14, backgroundColor: colors.green50 },
  notificationUnread: { position: 'absolute', right: 5, top: 5, width: 8, height: 8, borderRadius: 4, backgroundColor: colors.green500, borderWidth: 2, borderColor: colors.white },
  notificationCopy: { flex: 1, minWidth: 0 },
  notificationMeta: { marginBottom: 3, flexDirection: 'row', alignItems: 'center', gap: 6 },
  notificationLabel: { color: colors.green700, fontFamily: fonts.body, fontSize: 11, lineHeight: 15, fontWeight: '800' },
  notificationTime: { color: colors.ink400, fontFamily: fonts.body, fontSize: 10, lineHeight: 14 },
  notificationTitle: { color: colors.ink900, fontFamily: fonts.body, fontSize: 15, lineHeight: 20, fontWeight: '900', letterSpacing: -0.25 },
  notificationMessage: { marginTop: 3, color: colors.ink700, fontFamily: fonts.body, fontSize: 12, lineHeight: 17 },
  notificationActions: { minHeight: 76, alignItems: 'center', justifyContent: 'space-between' },
  notificationClose: { width: 44, height: 44, alignItems: 'center', justifyContent: 'center', borderRadius: 22, backgroundColor: colors.canvas },
  notificationOpen: { minHeight: 32, paddingHorizontal: 8, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 1, borderRadius: radius.control, backgroundColor: colors.canvas },
  notificationOpenText: { color: colors.ink900, fontFamily: fonts.body, fontSize: 11, fontWeight: '800' },
  notificationPressed: { opacity: 0.94, transform: [{ scale: 0.99 }] },
});
