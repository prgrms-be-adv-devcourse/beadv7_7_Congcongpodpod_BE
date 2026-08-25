import { RoundedIcon as Ionicons } from '@/components/rounded-icon';
import { router } from 'expo-router';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { EmptyState } from '@/components/empty-state';
import { LoadingState } from '@/components/loading-state';
import { Page } from '@/components/page';
import { colors, fonts, radius } from '@/constants/theme';
import { getReadNotificationIds, saveReadNotificationIds, temporaryNotifications, type InAppNotification } from '@/lib/in-app-notifications';
import { getNotifications, markAllNotificationsRead, markNotificationRead, notificationRoute, type ServerNotification } from '@/lib/notifications';

type DisplayNotification = InAppNotification & { serverId?: number; route?: string };
const iconByKind: Record<InAppNotification['kind'], keyof typeof Ionicons.glyphMap> = { ORDER: 'receipt-outline', PICKUP: 'bag-check-outline', BENEFIT: 'leaf-outline' };
const kindFromType = (type: string): InAppNotification['kind'] => type.includes('PICKUP') || type.includes('PICKED') ? 'PICKUP' : type.includes('ORDER') ? 'ORDER' : 'BENEFIT';
const relativeTime = (value: string) => {
  const elapsed = Date.now() - new Date(value).getTime();
  if (!Number.isFinite(elapsed) || elapsed < 60_000) return '방금';
  if (elapsed < 3_600_000) return `${Math.floor(elapsed / 60_000)}분 전`;
  if (elapsed < 86_400_000) return `${Math.floor(elapsed / 3_600_000)}시간 전`;
  return `${Math.floor(elapsed / 86_400_000)}일 전`;
};
const toDisplay = (notification: ServerNotification): DisplayNotification => ({ id: String(notification.id), serverId: notification.id, title: notification.title, message: notification.body, createdAt: relativeTime(notification.createdAt), kind: kindFromType(notification.type), route: notificationRoute(notification) });

export default function NotificationsScreen() {
  const [items, setItems] = useState<DisplayNotification[]>([]);
  const [readIds, setReadIds] = useState<Set<string>>(new Set());
  const [loading, setLoading] = useState(true);
  const [serverBacked, setServerBacked] = useState(false);
  const unreadCount = useMemo(() => items.filter(({ id }) => !readIds.has(id)).length, [items, readIds]);

  const load = useCallback(async () => {
    setLoading(true);
    const localReadIds = await getReadNotificationIds();
    try {
      const page = await getNotifications();
      setItems(page.items.map(toDisplay));
      setReadIds(new Set(page.items.filter(item => item.readYn).map(item => String(item.id))));
      setServerBacked(true);
    } catch {
      setItems(temporaryNotifications);
      setReadIds(localReadIds);
      setServerBacked(false);
    } finally { setLoading(false); }
  }, []);
  useEffect(() => { void load(); }, [load]);

  const markRead = useCallback(async (notification: DisplayNotification) => {
    if (!readIds.has(notification.id)) {
      const previous = readIds;
      const next = new Set(readIds).add(notification.id);
      setReadIds(next);
      if (serverBacked && notification.serverId) await markNotificationRead(notification.serverId).catch(() => setReadIds(previous));
      else await saveReadNotificationIds(next);
    }
    if (notification.route) router.push(notification.route as never);
  }, [readIds, serverBacked]);
  const markAllRead = useCallback(async () => {
    const previous = readIds;
    const next = new Set(items.map(({ id }) => id));
    setReadIds(next);
    if (serverBacked) await markAllNotificationsRead().catch(() => setReadIds(previous));
    else await saveReadNotificationIds(next);
  }, [items, readIds, serverBacked]);

  const action = unreadCount ? <Pressable accessibilityRole="button" onPress={() => void markAllRead()} style={({ pressed }) => [styles.readAll, pressed && styles.pressed]}><Text style={styles.readAllText}>모두 읽음</Text></Pressable> : null;
  return <Page title="알림" description={unreadCount ? `확인하지 않은 알림이 ${unreadCount}개 있어요.` : '새로운 알림이 없어요.'} action={action} onClose={() => router.replace('/')} closeLabel="홈으로 닫기">
    {loading ? <LoadingState label="알림을 확인하고 있어요"/> : items.length ? <View style={styles.list}>{items.map(notification => {
      const read = readIds.has(notification.id);
      return <Pressable accessibilityRole="button" accessibilityState={{ selected: !read }} key={notification.id} onPress={() => void markRead(notification)} style={({ pressed }) => [styles.item, !read && styles.unreadItem, pressed && styles.pressed]}>
        <View style={[styles.icon, !read && styles.unreadIcon]}><Ionicons name={iconByKind[notification.kind]} size={19} color={read ? colors.ink700 : colors.green700}/></View>
        <View style={styles.copy}><View style={styles.titleRow}><Text style={[styles.itemTitle, read && styles.readTitle]}>{notification.title}</Text>{!read ? <View accessibilityLabel="읽지 않음" style={styles.dot}/> : null}</View><Text style={styles.message}>{notification.message}</Text><Text style={styles.time}>{notification.createdAt}</Text></View>
      </Pressable>;
    })}</View> : <EmptyState title="아직 알림이 없어요" description="주문과 픽업 소식이 생기면 여기에 알려드릴게요."/>}
  </Page>;
}

const styles = StyleSheet.create({
  readAll: { minHeight: 44, paddingHorizontal: 6, alignItems: 'center', justifyContent: 'center' }, readAllText: { color: colors.green700, fontFamily: fonts.body, fontSize: 13, fontWeight: '800' },
  list: { overflow: 'hidden', backgroundColor: colors.white, borderRadius: radius.card, borderWidth: 1, borderColor: colors.line }, item: { minHeight: 104, paddingHorizontal: 16, paddingVertical: 15, flexDirection: 'row', gap: 13, borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: colors.line }, unreadItem: { backgroundColor: colors.green50 },
  icon: { width: 38, height: 38, borderRadius: radius.control, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.canvas }, unreadIcon: { backgroundColor: colors.green100 }, copy: { flex: 1 }, titleRow: { flexDirection: 'row', alignItems: 'center', gap: 7 }, itemTitle: { flex: 1, color: colors.ink900, fontFamily: fonts.body, fontSize: 15, lineHeight: 21, fontWeight: '800', letterSpacing: -0.25 }, readTitle: { color: colors.ink700, fontWeight: '700' }, dot: { width: 7, height: 7, borderRadius: 4, backgroundColor: colors.green500 }, message: { marginTop: 5, color: colors.ink700, fontFamily: fonts.body, fontSize: 13, lineHeight: 19 }, time: { marginTop: 8, color: colors.ink400, fontFamily: fonts.body, fontSize: 11, fontWeight: '600' }, pressed: { opacity: 0.7 },
});
