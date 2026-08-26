import { useFocusEffect, useLocalSearchParams } from 'expo-router';
import { useCallback, useEffect, useRef, useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { ConfirmModal } from '@/components/confirm-modal';
import { EmptyState } from '@/components/empty-state';
import { LoadingState } from '@/components/loading-state';
import { SellerShell } from '@/components/seller-shell';
import { colors, fonts, radius } from '@/constants/theme';
import { usePullToRefresh } from '@/hooks/use-pull-to-refresh';
import { showAppAlert } from '@/lib/app-overlay';
import { subscribeOrderStateChanged } from '@/lib/order-events';
import { acceptStoreOrder, getMyStores, getStoreOrders, rejectStoreOrder, updateStorePickup, type SellerOrder } from '@/lib/seller';

const tabs = [['RESERVED', '접수 대기'], ['PICKUP_READY', '픽업 대기'], ['PICKED_UP', '완료'], ['CLOSED', '종료']] as const;
type Tab = (typeof tabs)[number][0];
type PendingAction = { order: SellerOrder; status: 'ACCEPT' | 'PICKED_UP' | 'NO_SHOW' };
const reasons = [['OUT_OF_STOCK', '재고 소진'], ['QUALITY_ISSUE', '상품 품질 문제'], ['NOT_READY', '준비 어려움'], ['STORE_CLOSED', '영업 종료']] as const;

export default function SellerOrders() {
  const params = useLocalSearchParams<{ tab?: string }>();
  const [tab, setTab] = useState<Tab>(isTab(params.tab) ? params.tab : 'RESERVED');
  const [storeId, setStoreId] = useState<number>();
  const [storeName, setStoreName] = useState('미등록');
  const [ordersByTab, setOrdersByTab] = useState<Partial<Record<Tab, SellerOrder[]>>>({});
  const [failedByTab, setFailedByTab] = useState<Partial<Record<Tab, boolean>>>({});
  const [loadingTab, setLoadingTab] = useState<Tab>();
  const [storeFailed, setStoreFailed] = useState(false);
  const [processing, setProcessing] = useState<number>();
  const [rejecting, setRejecting] = useState<number>();
  const [pendingAction, setPendingAction] = useState<PendingAction>();
  const [codes, setCodes] = useState<Record<number, string>>({});
  const actionTimer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);

  useEffect(() => () => { if (actionTimer.current) clearTimeout(actionTimer.current); }, []);

  useEffect(() => {
    if (isTab(params.tab)) setTab(params.tab);
  }, [params.tab]);

  useEffect(() => {
    void getMyStores()
      .then(([store]) => {
        setStoreFailed(false);
        if (!store) return;
        setStoreId(store.storeId);
        setStoreName(store.storeName);
      })
      .catch(() => setStoreFailed(true));
  }, []);

  const load = useCallback(async () => {
    if (!storeId) return;
    const requestedTab = tab;
    setLoadingTab(requestedTab);
    try {
      const rows = await getStoreOrders(storeId, requestedTab === 'CLOSED' ? undefined : requestedTab);
      const next = requestedTab === 'CLOSED' ? rows.filter((order) => ['CANCELLED', 'REJECTED', 'NO_SHOW'].includes(order.status)) : rows;
      setOrdersByTab((current) => ({ ...current, [requestedTab]: next }));
      setFailedByTab((current) => ({ ...current, [requestedTab]: false }));
    } catch {
      setFailedByTab((current) => ({ ...current, [requestedTab]: true }));
    } finally {
      setLoadingTab((current) => current === requestedTab ? undefined : current);
    }
  }, [storeId, tab]);

  useFocusEffect(useCallback(() => { void load(); }, [load]));
  useEffect(() => subscribeOrderStateChanged(() => { void load(); }), [load]);
  const { refreshing, onRefresh } = usePullToRefresh(load);
  const orders = ordersByTab[tab] ?? [];
  const hasLoaded = Object.prototype.hasOwnProperty.call(ordersByTab, tab);
  const failed = Boolean(failedByTab[tab]);
  const loading = loadingTab === tab;

  const removeOrder = (orderId: number) => setOrdersByTab((current) => ({ ...current, [tab]: (current[tab] ?? []).filter((order) => order.orderId !== orderId) }));
  const accept = async (order: SellerOrder) => {
    try {
      setProcessing(order.orderId);
      const result = await acceptStoreOrder(order.orderId);
      setCodes((current) => ({ ...current, [order.orderId]: result.pickUpCode }));
      removeOrder(order.orderId);
      setPendingAction(undefined);
      setTab('PICKUP_READY');
    } catch (error) {
      showAppAlert('접수하지 못했어요', error instanceof Error ? error.message : '잠시 후 다시 시도해주세요.');
    } finally {
      setProcessing(undefined);
    }
  };
  const reject = async (order: SellerOrder, reason: (typeof reasons)[number][0]) => {
    try {
      setProcessing(order.orderId);
      await rejectStoreOrder(order.orderId, reason);
      setRejecting(undefined);
      removeOrder(order.orderId);
    } catch (error) {
      showAppAlert('거절하지 못했어요', error instanceof Error ? error.message : '잠시 후 다시 시도해주세요.');
    } finally {
      setProcessing(undefined);
    }
  };
  const pickup = async (order: SellerOrder, status: 'PICKED_UP' | 'NO_SHOW') => {
    try {
      setProcessing(order.orderId);
      await updateStorePickup(order.orderId, status);
      removeOrder(order.orderId);
      setPendingAction(undefined);
    } catch (error) {
      showAppAlert('처리하지 못했어요', error instanceof Error ? error.message : '잠시 후 다시 시도해주세요.');
    } finally {
      setProcessing(undefined);
    }
  };
  const confirmAction = () => {
    if (!pendingAction) return;
    const action = pendingAction;
    setProcessing(action.order.orderId);
    setPendingAction(undefined);
    // 네이티브 Modal의 fade-out이 끝난 뒤 결과 알림을 열어 두 Modal이
    // 겹치거나 오류 알림이 확인창 아래에 쌓이지 않게 한다.
    actionTimer.current = setTimeout(() => {
      if (action.status === 'ACCEPT') void accept(action.order);
      else void pickup(action.order, action.status);
    }, 220);
  };
  const confirmCopy = pendingAction?.status === 'ACCEPT'
    ? { icon: 'receipt-outline' as const, title: '주문을 접수할까요?', description: `${pendingAction.order.dishName} ${pendingAction.order.quantity}개를 준비합니다.\n접수하면 고객에게 픽업 코드가 발급돼요.`, label: '주문 접수', busy: '접수 중…' }
    : pendingAction?.status === 'PICKED_UP'
      ? { icon: 'checkmark-circle-outline' as const, title: '픽업을 완료할까요?', description: `주문 #${pendingAction.order.orderId}의 고객 코드를 확인하셨나요?\n완료 후에는 상태를 되돌릴 수 없어요.`, label: '픽업 완료', busy: '완료 처리 중…' }
      : { icon: 'alert-circle-outline' as const, title: '미수령 처리할까요?', description: `주문 #${pendingAction?.order.orderId ?? ''}을 노쇼로 처리합니다.\n고객이 방문하지 않은 경우에만 진행해주세요.`, label: '미수령 처리', busy: '미수령 처리 중…' };

  return <>
    <SellerShell title="주문 관리" description="픽업 시간이 가까운 주문부터 확인하고 상태를 처리하세요." storeName={storeName} refreshing={refreshing} onRefresh={onRefresh}>
      <View accessibilityRole="tablist" style={styles.tabs}>{tabs.map(([key, label]) => <Pressable accessibilityRole="tab" accessibilityState={{ selected: tab === key }} key={key} onPress={() => setTab(key)} style={styles.tab}><Text style={[styles.tabText, tab === key && styles.active]}>{label}</Text>{tab === key ? <View style={styles.tabLine}/> : null}</Pressable>)}</View>
      {storeFailed ? <EmptyState title="매장 정보를 불러오지 못했어요" description="네트워크 상태를 확인한 뒤 다시 들어와주세요."/> : loading && !hasLoaded ? <LoadingState label="매장 주문을 확인하고 있어요" compact/> : failed && !orders.length ? <EmptyState title="주문을 불러오지 못했어요" description="기존 주문은 유지되며 다시 불러올 수 있어요." actionLabel="다시 불러오기" onAction={() => void load()}/> : <>
        {failed ? <Pressable accessibilityRole="button" onPress={() => void load()} style={({ pressed }) => [styles.errorNotice, pressed && styles.pressed]}><Text style={styles.errorNoticeText}>최신 주문을 갱신하지 못했어요.</Text><Text style={styles.errorRetry}>재시도</Text></Pressable> : null}
        {orders.length ? <View style={styles.orderList}>{orders.map((order, index) => <OrderRow busy={processing === order.orderId} code={codes[order.orderId]} key={order.orderId} last={index === orders.length - 1} onAccept={() => setPendingAction({ order, status: 'ACCEPT' })} onPickup={(status) => setPendingAction({ order, status })} onReject={(reason) => void reject(order, reason)} onRejectOpen={() => setRejecting((current) => current === order.orderId ? undefined : order.orderId)} order={order} rejecting={rejecting === order.orderId}/>)}</View> : <EmptyState title="이 상태의 주문이 없어요" description="새 주문이 들어오면 픽업 시간 순으로 표시됩니다."/>}
      </>}
    </SellerShell>
    <ConfirmModal visible={Boolean(pendingAction)} icon={confirmCopy.icon} title={confirmCopy.title} description={confirmCopy.description} confirmLabel={confirmCopy.label} busy={Boolean(processing)} busyLabel={confirmCopy.busy} onCancel={() => { if (!processing) setPendingAction(undefined); }} onConfirm={confirmAction}/>
  </>;
}

function OrderRow({ order, code, busy, rejecting, last, onAccept, onRejectOpen, onReject, onPickup }: { order: SellerOrder; code?: string; busy: boolean; rejecting: boolean; last: boolean; onAccept: () => void; onRejectOpen: () => void; onReject: (reason: (typeof reasons)[number][0]) => void; onPickup: (status: 'PICKED_UP' | 'NO_SHOW') => void }) {
  const status = statusVisual(order.status);
  return <View style={[styles.order, last && styles.orderLast]}>
    <View style={styles.orderTop}>
      <View style={styles.statusRow}><View style={[styles.stateDot, { backgroundColor: status.color }]}/><Text style={[styles.state, { color: status.color }]}>{status.label}</Text><Text style={styles.orderId}>#{order.orderId}</Text></View>
      <View style={styles.pickupBlock}><Text style={styles.pickup}>{formatPickupTime(order.pickupStartAt)}–{formatPickupTime(order.pickupEndAt)}</Text><Text style={styles.pickupLabel}>픽업</Text></View>
    </View>
    <View style={styles.orderMain}><View style={styles.orderCopy}><Text numberOfLines={1} style={styles.menu}>{order.dishName} × {order.quantity}</Text><Text numberOfLines={1} style={styles.customer}>{order.memberName ?? '고객'} · {order.phone ?? '연락처 없음'}</Text></View><Text style={styles.priceLine}>{Number(order.totalPrice).toLocaleString()}원</Text></View>
    {order.status === 'PICKUP_READY' ? <View style={styles.codeBox}><View><Text style={styles.codeLabel}>고객 픽업 코드</Text><Text style={styles.code}>{code ?? order.pickupCode ?? '고객 화면에서 확인'}</Text></View><Text style={styles.codeHint}>확인 후 완료 처리</Text></View> : null}
    {order.status === 'RESERVED' ? <><View style={styles.actions}><Pressable accessibilityRole="button" disabled={busy} onPress={onRejectOpen} style={({ pressed }) => [styles.secondary, pressed && styles.pressed]}><Text style={styles.secondaryText}>주문 거절</Text></Pressable><Pressable accessibilityRole="button" disabled={busy} onPress={onAccept} style={({ pressed }) => [styles.action, pressed && styles.pressed]}><Text style={styles.actionText}>{busy ? '처리 중…' : '주문 접수'}</Text></Pressable></View>{rejecting ? <View style={styles.reasons}><Text style={styles.reasonTitle}>거절 사유를 선택하세요</Text><View style={styles.reasonGrid}>{reasons.map(([key, label]) => <Pressable accessibilityRole="button" key={key} disabled={busy} onPress={() => onReject(key)} style={({ pressed }) => [styles.reason, pressed && styles.pressed]}><Text style={styles.reasonText}>{label}</Text></Pressable>)}</View></View> : null}</> : order.status === 'PICKUP_READY' ? <View style={styles.actions}><Pressable accessibilityRole="button" disabled={busy} onPress={() => onPickup('NO_SHOW')} style={({ pressed }) => [styles.secondary, pressed && styles.pressed]}><Text style={styles.secondaryText}>미수령 처리</Text></Pressable><Pressable accessibilityRole="button" disabled={busy} onPress={() => onPickup('PICKED_UP')} style={({ pressed }) => [styles.action, pressed && styles.pressed]}><Text style={styles.actionText}>{busy ? '처리 중…' : '코드 확인 · 픽업 완료'}</Text></Pressable></View> : null}
  </View>;
}

function isTab(value?: string): value is Tab {
  return tabs.some(([key]) => key === value);
}

function formatPickupTime(value?: string) {
  const match = (value ?? '').match(/(\d{2}):(\d{2})/);
  return match ? `${match[1]}:${match[2]}` : '--:--';
}

function statusVisual(status: string) {
  if (status === 'RESERVED') return { label: '접수 대기', color: colors.warning };
  if (status === 'PICKUP_READY') return { label: '픽업 대기', color: colors.green700 };
  if (status === 'PICKED_UP') return { label: '픽업 완료', color: colors.ink700 };
  if (status === 'NO_SHOW') return { label: '미수령', color: colors.danger700 };
  if (status === 'REJECTED') return { label: '거절', color: colors.danger700 };
  return { label: '취소', color: colors.ink500 };
}

const styles = StyleSheet.create({
  tabs: { flexDirection: 'row', borderBottomWidth: 1, borderBottomColor: colors.line },
  tab: { flex: 1, minHeight: 48, alignItems: 'center', justifyContent: 'center' },
  tabText: { color: colors.ink400, fontFamily: fonts.body, fontSize: 12, fontWeight: '700' },
  active: { color: colors.ink900, fontWeight: '900' },
  tabLine: { position: 'absolute', left: '50%', bottom: 0, width: 42, height: 3, marginLeft: -21, borderRadius: 2, backgroundColor: colors.ink900 },
  errorNotice: { minHeight: 44, paddingHorizontal: 12, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', borderRadius: radius.control, backgroundColor: colors.apricot50, borderWidth: 1, borderColor: colors.apricot300 },
  errorNoticeText: { color: colors.ink700, fontFamily: fonts.body, fontSize: 11, fontWeight: '700' },
  errorRetry: { color: colors.ink900, fontFamily: fonts.body, fontSize: 11, fontWeight: '900' },
  orderList: { overflow: 'hidden', borderRadius: radius.card, borderWidth: 1, borderColor: colors.line, backgroundColor: colors.white },
  order: { padding: 14, borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: colors.line },
  orderLast: { borderBottomWidth: 0 },
  orderTop: { flexDirection: 'row', alignItems: 'flex-start', justifyContent: 'space-between', gap: 12 },
  statusRow: { minHeight: 28, flexDirection: 'row', alignItems: 'center', gap: 6 },
  stateDot: { width: 6, height: 6, borderRadius: 3 },
  state: { fontFamily: fonts.body, fontSize: 11, fontWeight: '800' },
  orderId: { color: colors.ink400, fontFamily: fonts.body, fontSize: 10, fontWeight: '700' },
  pickupBlock: { alignItems: 'flex-end' },
  pickup: { color: colors.ink900, fontFamily: fonts.body, fontSize: 15, fontWeight: '900', fontVariant: ['tabular-nums'] },
  pickupLabel: { marginTop: 2, color: colors.ink400, fontFamily: fonts.body, fontSize: 9 },
  orderMain: { marginTop: 8, flexDirection: 'row', alignItems: 'flex-end', gap: 12 },
  orderCopy: { flex: 1, minWidth: 0 },
  menu: { color: colors.ink900, fontFamily: fonts.body, fontSize: 14, fontWeight: '800' },
  customer: { marginTop: 4, color: colors.ink500, fontFamily: fonts.body, fontSize: 11 },
  priceLine: { color: colors.ink900, fontFamily: fonts.body, fontSize: 13, fontWeight: '900', fontVariant: ['tabular-nums'] },
  codeBox: { marginTop: 11, minHeight: 60, paddingHorizontal: 12, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 12, borderRadius: radius.control, backgroundColor: colors.blue50, borderWidth: 1, borderColor: colors.blue300 },
  codeLabel: { color: colors.ink700, fontFamily: fonts.body, fontSize: 9 },
  code: { marginTop: 3, color: colors.ink900, fontFamily: fonts.body, fontSize: 18, fontWeight: '900', letterSpacing: 0.8, fontVariant: ['tabular-nums'] },
  codeHint: { color: colors.ink500, fontFamily: fonts.body, fontSize: 9 },
  actions: { marginTop: 11, flexDirection: 'row', gap: 8 },
  secondary: { flex: 1, minHeight: 44, alignItems: 'center', justifyContent: 'center', borderRadius: radius.control, borderWidth: 1, borderColor: colors.lineStrong, backgroundColor: colors.white },
  secondaryText: { color: colors.ink700, fontFamily: fonts.body, fontSize: 11, fontWeight: '800' },
  action: { flex: 1.45, minHeight: 44, alignItems: 'center', justifyContent: 'center', borderRadius: radius.control, backgroundColor: colors.green500 },
  actionText: { color: colors.white, fontFamily: fonts.body, fontSize: 11, fontWeight: '900' },
  reasons: { marginTop: 9, padding: 10, borderRadius: radius.control, backgroundColor: colors.canvas },
  reasonTitle: { marginBottom: 8, color: colors.ink700, fontFamily: fonts.body, fontSize: 10 },
  reasonGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: 6 },
  reason: { minHeight: 40, paddingHorizontal: 10, alignItems: 'center', justifyContent: 'center', borderRadius: radius.control, backgroundColor: colors.white, borderWidth: 1, borderColor: colors.line },
  reasonText: { color: colors.ink900, fontFamily: fonts.body, fontSize: 10, fontWeight: '700' },
  pressed: { opacity: 0.68, transform: [{ scale: 0.99 }] },
});
