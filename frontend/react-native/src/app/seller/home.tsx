import { RoundedIcon as Ionicons } from '@/components/rounded-icon';
import { router, useFocusEffect } from 'expo-router';
import { useCallback, useMemo, useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { OptimizedImage as Image } from '@/components/optimized-image';

import { ConfirmModal } from '@/components/confirm-modal';
import { EmptyState } from '@/components/empty-state';
import { LoadingState } from '@/components/loading-state';
import { PrimaryButton } from '@/components/page';
import { SellerShell } from '@/components/seller-shell';
import { colors, fonts, radius, shadow } from '@/constants/theme';
import { usePullToRefresh } from '@/hooks/use-pull-to-refresh';
import { useResponsiveLayout } from '@/hooks/use-responsive-layout';
import { getDishImageSource, getStoreProfileImageSource } from '@/lib/food-image';
import { showAppAlert } from '@/lib/app-overlay';
import { changeStoreStatus, getMyStores, getSellerDishes, getSettlements, getStoreOrders, getStoreSales, type SellerOrder, type Settlement } from '@/lib/seller';
import { getStoreCategoryVisual } from '@/lib/store-category';
import type { Dish, Store } from '@/types/store';

const LOW_STOCK_MAX = 5;

export default function SellerHome() {
  const { isTablet } = useResponsiveLayout();
  const [store, setStore] = useState<Store | null>(null);
  const [orders, setOrders] = useState<SellerOrder[]>([]);
  const [dishes, setDishes] = useState<Dish[]>([]);
  const [todaySales, setTodaySales] = useState(0);
  const [latestSettlement, setLatestSettlement] = useState<Settlement | null>(null);
  const [salesLabel, setSalesLabel] = useState('오늘 매출');
  const [loading, setLoading] = useState(true);
  const [failed, setFailed] = useState(false);
  const [partialError, setPartialError] = useState(false);
  const [statusChanging, setStatusChanging] = useState(false);
  const [pendingStatus, setPendingStatus] = useState<'OPEN' | 'CLOSED'>();

  const load = useCallback(async () => {
    setLoading(true);
    setFailed(false);
    setPartialError(false);
    try {
      const [mine] = await getMyStores();
      if (!mine) {
        setStore(null);
        setOrders([]);
        setDishes([]);
        return;
      }
      setStore(mine);
      const [nextOrders, nextDishes, sales, settlements] = await Promise.all([
        getStoreOrders(mine.storeId).catch(() => null),
        getSellerDishes(mine.storeId).catch(() => null),
        getStoreSales(mine.storeId).catch(() => null),
        getSettlements().catch(() => null),
      ]);
      if (nextOrders) setOrders(nextOrders);
      else setPartialError(true);
      if (nextDishes) setDishes(nextDishes);
      else setPartialError(true);
      if (sales) {
        setTodaySales(sales.salesAmount);
        setSalesLabel('오늘 매출');
      } else if (nextOrders) {
        setTodaySales(nextOrders.filter((order) => !['CANCELLED', 'REJECTED'].includes(order.status)).reduce((sum, order) => sum + Number(order.totalPrice), 0));
        setSalesLabel('최근 주문 금액');
      }
      if (settlements) setLatestSettlement(settlements.find((item) => item.storeId === mine.storeId) ?? null);
    } catch {
      setFailed(true);
    } finally {
      setLoading(false);
    }
  }, []);

  useFocusEffect(useCallback(() => { void load(); }, [load]));
  const { refreshing, onRefresh } = usePullToRefresh(load);

  const applyStatusChange = useCallback(async () => {
    if (!store || !pendingStatus || statusChanging) return;
    try {
      setStatusChanging(true);
      setStore(await changeStoreStatus(store.storeId, pendingStatus));
      setPendingStatus(undefined);
    } catch (error) {
      showAppAlert('운영 상태를 변경하지 못했어요', error instanceof Error ? error.message : '잠시 후 다시 시도해주세요.');
    } finally {
      setStatusChanging(false);
    }
  }, [pendingStatus, statusChanging, store]);

  const pendingCount = orders.filter((order) => order.status === 'RESERVED').length;
  const readyCount = orders.filter((order) => order.status === 'PICKUP_READY').length;
  const completedCount = orders.filter((order) => order.status === 'PICKED_UP').length;
  const soldOutCount = dishes.filter((dish) => dish.quantity === 0 || dish.status === 'SOLD_OUT').length;
  const urgentOrders = useMemo(() => orders
    .filter((order) => order.status === 'RESERVED' || order.status === 'PICKUP_READY')
    .sort((a, b) => pickupMinutes(a) - pickupMinutes(b))
    .slice(0, 3), [orders]);
  const inventoryAlerts = useMemo(() => dishes
    .filter((dish) => dish.quantity <= LOW_STOCK_MAX || dish.status === 'SOLD_OUT')
    .sort((a, b) => a.quantity - b.quantity)
    .slice(0, 4), [dishes]);

  if (loading && !store) return <SellerShell title="대시보드" description="오늘 운영 현황을 준비하고 있어요." storeName="내 매장"><LoadingState compact label="매장 현황을 불러오고 있어요"/></SellerShell>;
  if (failed && !store) return <SellerShell title="대시보드" description="매장 운영 정보를 불러오지 못했어요." storeName="내 매장" refreshing={refreshing} onRefresh={onRefresh}><EmptyState title="대시보드를 열지 못했어요" description="네트워크 상태를 확인한 뒤 다시 시도해주세요." actionLabel="다시 불러오기" onAction={() => void load()}/></SellerShell>;
  if (!store) return <SellerShell title="판매를 시작해볼까요?" description="상점을 등록하면 상품·주문·정산을 한곳에서 관리할 수 있어요." storeName="미등록" refreshing={refreshing} onRefresh={onRefresh}><View style={styles.onboarding}><Text style={styles.onboardingTitle}>첫 매장을 등록해주세요</Text><Text style={styles.onboardingBody}>사업자 정보와 매장 위치를 입력하면 판매자 권한을 확인한 뒤 대시보드가 열려요.</Text></View><PrimaryButton label="상점 등록하기" onPress={() => router.push('/seller/store')}/></SellerShell>;

  return <SellerShell title="대시보드" description="지금 처리할 주문과 오늘 운영 상태를 빠르게 확인하세요." storeName={store.storeName} storeStatus={store.status} refreshing={refreshing} onRefresh={onRefresh}>
    <StoreSummary store={store} statusChanging={statusChanging} onManage={() => router.push('/seller/store')} onStatusChange={() => setPendingStatus(store.status === 'OPEN' ? 'CLOSED' : 'OPEN')}/>
    {partialError ? <Pressable accessibilityRole="button" onPress={() => void load()} style={({ pressed }) => [styles.partialNotice, pressed && styles.pressed]}><Ionicons name="cloud-offline-outline" size={17} color={colors.warning}/><Text style={styles.partialNoticeText}>일부 운영 정보를 갱신하지 못했어요.</Text><Text style={styles.retryText}>재시도</Text></Pressable> : null}
    <UrgentOrders orders={urgentOrders} pendingCount={pendingCount} readyCount={readyCount}/>
    <View style={[styles.insightGrid, isTablet && styles.insightGridWide]}>
      <RevenueHero amount={todaySales} label={salesLabel} settlement={latestSettlement} wide={isTablet}/>
      <View style={[styles.supportingMetrics, isTablet && styles.supportingMetricsWide]}>
        <SupportingMetric label="픽업 완료" value={completedCount} unit="건"/>
        <View style={styles.metricDivider}/>
        <SupportingMetric label="등록 상품" value={dishes.length} unit="개"/>
        <View style={styles.metricDivider}/>
        <SupportingMetric label="품절" value={soldOutCount} unit="개" alert={soldOutCount > 0}/>
      </View>
    </View>
    <InventoryWarnings category={store.category} dishes={inventoryAlerts}/>
    <ConfirmModal visible={Boolean(pendingStatus)} icon={pendingStatus === 'OPEN' ? 'storefront-outline' : 'moon-outline'} title={pendingStatus === 'OPEN' ? '매장을 열까요?' : '매장을 닫을까요?'} description={pendingStatus === 'OPEN' ? '변경 즉시 고객의 지도와 목록에 매장이 노출됩니다.' : '변경 즉시 고객의 지도와 목록에서 매장이 숨겨집니다.'} confirmLabel={pendingStatus === 'OPEN' ? '매장 열기' : '매장 닫기'} busy={statusChanging} onCancel={() => setPendingStatus(undefined)} onConfirm={() => void applyStatusChange()}/>
  </SellerShell>;
}

function StoreSummary({ store, statusChanging, onManage, onStatusChange }: { store: Store; statusChanging: boolean; onManage: () => void; onStatusChange: () => void }) {
  const category = getStoreCategoryVisual(store.category);
  const statusLabel = store.status === 'OPEN' ? '운영 중' : store.status === 'STOPPED' ? '운영 중지' : '오픈 전';
  const hours = store.openTime && store.closeTime ? `${store.openTime.slice(0, 5)}–${store.closeTime.slice(0, 5)}` : '영업시간 미등록';
  return <View style={styles.operation}>
    <Image source={getStoreProfileImageSource(store)} style={styles.storeProfile}/>
    <View style={styles.storeSummaryCopy}>
      <View style={styles.storeNameRow}><Text numberOfLines={1} style={styles.storeSummaryName}>{store.storeName}</Text><Text style={styles.storeCategory}>{category.label}</Text></View>
      <Text numberOfLines={1} style={styles.storeMeta}>{store.address || '주소 미등록'} · {hours}</Text>
      <View style={styles.operationTitleRow}><View style={[styles.operationDot, store.status !== 'OPEN' && styles.operationDotClosed]}/><Text style={styles.operationTitle}>{statusLabel}</Text></View>
    </View>
    <Pressable accessibilityRole="button" accessibilityLabel="매장 정보 관리" onPress={onManage} style={({ pressed }) => [styles.manageAction, pressed && styles.pressed]}><Text style={styles.manageText}>관리</Text></Pressable>
    <Pressable accessibilityRole="button" disabled={statusChanging || store.status === 'STOPPED'} onPress={onStatusChange} style={({ pressed }) => [styles.statusAction, store.status === 'OPEN' && styles.statusActionClose, (pressed || statusChanging || store.status === 'STOPPED') && styles.statusActionDisabled]}><Text style={[styles.statusActionText, store.status === 'OPEN' && styles.statusActionCloseText]}>{store.status === 'OPEN' ? '닫기' : store.status === 'STOPPED' ? '변경 불가' : '열기'}</Text></Pressable>
  </View>;
}

function UrgentOrders({ orders, pendingCount, readyCount }: { orders: SellerOrder[]; pendingCount: number; readyCount: number }) {
  const total = pendingCount + readyCount;
  return <View style={styles.priority}>
    <View style={styles.priorityHeader}>
      <View><View style={styles.priorityKicker}><View style={styles.priorityDot}/><Text style={styles.priorityKickerText}>긴급 주문</Text></View><Text style={styles.priorityTitle}>{total ? `${total}건을 확인해주세요` : '처리할 주문이 없습니다'}</Text></View>
      <Pressable accessibilityRole="button" onPress={() => router.push('/seller/orders')} style={({ pressed }) => [styles.priorityLink, pressed && styles.pressed]}><Text style={styles.priorityLinkText}>전체 보기</Text><Ionicons name="chevron-forward" size={16} color={colors.white}/></Pressable>
    </View>
    {orders.length ? <View style={styles.queue}>{orders.map((order) => <UrgentOrderRow key={order.orderId} order={order}/>)}</View> : <Text style={styles.queueEmpty}>새 주문이 들어오면 픽업 시간 순으로 보여드려요.</Text>}
  </View>;
}

function UrgentOrderRow({ order }: { order: SellerOrder }) {
  const ready = order.status === 'PICKUP_READY';
  return <Pressable accessibilityRole="button" onPress={() => router.push({ pathname: '/seller/orders', params: { tab: order.status } })} style={({ pressed }) => [styles.queueRow, pressed && styles.queueRowPressed]}>
    <View style={styles.queueTime}><Text style={styles.queueTimeValue}>{formatPickupTime(order.pickupEndAt)}</Text><Text style={styles.queueTimeLabel}>마감</Text></View>
    <View style={styles.queueCopy}><Text style={styles.queueDish} numberOfLines={1}>{order.dishName} × {order.quantity}</Text><Text style={styles.queueMeta}>주문 #{order.orderId} · {ready ? '픽업 대기' : '접수 대기'}</Text></View>
    <Ionicons name="chevron-forward" size={17} color={colors.ink400}/>
  </Pressable>;
}

function RevenueHero({ amount, label, settlement, wide }: { amount: number; label: string; settlement: Settlement | null; wide: boolean }) {
  const context = settlement ? `${settlement.settlementMonth.replace('-', '.')} 정산 ${settlementStatusLabel(settlement.status)}` : '정산 내역은 정산 탭에서 확인할 수 있어요.';
  return <View style={[styles.revenueHero, wide && styles.revenueHeroWide]}><Text style={styles.revenueLabel}>{label}</Text><Text numberOfLines={1} adjustsFontSizeToFit style={styles.revenueValue}>{amount.toLocaleString()}<Text style={styles.revenueUnit}>원</Text></Text><Text style={styles.revenueContext}>{context}</Text></View>;
}

function SupportingMetric({ label, value, unit, alert }: { label: string; value: number; unit: string; alert?: boolean }) {
  return <View style={styles.supportingMetric}><Text style={styles.supportingLabel}>{label}</Text><Text style={[styles.supportingValue, alert && styles.supportingAlert]}>{value}<Text style={styles.supportingUnit}>{unit}</Text></Text></View>;
}

function InventoryWarnings({ dishes, category }: { dishes: Dish[]; category?: string }) {
  return <View>
    <View style={styles.sectionHead}><View><Text style={styles.sectionTitle}>재고 확인</Text><Text style={styles.sectionDescription}>품절 또는 {LOW_STOCK_MAX}개 이하 상품을 먼저 보여드려요.</Text></View><Pressable accessibilityRole="button" onPress={() => router.push('/seller/dishes')} style={styles.sectionLinkHit}><Text style={styles.sectionLink}>상품 관리</Text></Pressable></View>
    <View style={styles.inventorySurface}>{dishes.length ? dishes.map((dish, index) => <Pressable accessibilityRole="button" key={dish.dishId} onPress={() => router.push('/seller/dishes')} style={({ pressed }) => [styles.inventoryRow, index === dishes.length - 1 && styles.inventoryRowLast, pressed && styles.inventoryPressed]}>
      <Image source={getDishImageSource(dish, category)} style={styles.inventoryImage}/>
      <View style={styles.inventoryCopy}><Text numberOfLines={1} style={styles.inventoryName}>{dish.dishName}</Text><Text style={[styles.inventoryStock, dish.quantity === 0 && styles.inventorySoldOut]}>{dish.quantity === 0 ? '품절' : `재고 ${dish.quantity}개`}</Text></View>
      <Text style={styles.inventoryAction}>조정</Text>
    </Pressable>) : <View style={styles.inventoryClear}><Ionicons name="checkmark-circle-outline" size={21} color={colors.green700}/><View><Text style={styles.inventoryClearTitle}>재고 상태가 안정적입니다</Text><Text style={styles.inventoryClearBody}>현재 품절·재고 부족 상품이 없어요.</Text></View></View>}</View>
  </View>;
}

function settlementStatusLabel(status: string) {
  if (status === 'COMPLETED') return '지급 완료';
  if (status === 'FAILED') return '확인 필요';
  return '처리 중';
}

function pickupMinutes(order: SellerOrder) {
  const match = (order.pickupEndAt ?? order.pickupStartAt ?? '').match(/(\d{2}):(\d{2})/);
  return match ? Number(match[1]) * 60 + Number(match[2]) : Number.MAX_SAFE_INTEGER;
}

function formatPickupTime(value?: string) {
  const match = (value ?? '').match(/(\d{2}):(\d{2})/);
  return match ? `${match[1]}:${match[2]}` : '--:--';
}

const styles = StyleSheet.create({
  operation: { minHeight: 82, padding: 12, flexDirection: 'row', alignItems: 'center', gap: 10, borderRadius: radius.card, backgroundColor: colors.white, borderWidth: 1, borderColor: colors.line },
  storeProfile: { width: 54, height: 54, borderRadius: 14, backgroundColor: colors.canvas },
  storeSummaryCopy: { flex: 1, minWidth: 0 },
  storeNameRow: { flexDirection: 'row', alignItems: 'center', gap: 7 },
  storeSummaryName: { flexShrink: 1, color: colors.ink900, fontFamily: fonts.body, fontSize: 15, fontWeight: '900' },
  storeCategory: { color: colors.ink500, fontFamily: fonts.body, fontSize: 9, fontWeight: '700' },
  storeMeta: { marginTop: 5, color: colors.ink500, fontFamily: fonts.body, fontSize: 10 },
  operationTitleRow: { marginTop: 5, flexDirection: 'row', alignItems: 'center', gap: 5 },
  operationDot: { width: 6, height: 6, borderRadius: 3, backgroundColor: colors.green500 },
  operationDotClosed: { backgroundColor: colors.ink400 },
  operationTitle: { color: colors.ink700, fontFamily: fonts.body, fontSize: 10, fontWeight: '800' },
  manageAction: { minWidth: 44, minHeight: 44, alignItems: 'center', justifyContent: 'center', borderRadius: radius.control, backgroundColor: colors.canvas },
  manageText: { color: colors.ink900, fontFamily: fonts.body, fontSize: 11, fontWeight: '800' },
  statusAction: { minWidth: 54, minHeight: 44, paddingHorizontal: 10, alignItems: 'center', justifyContent: 'center', borderRadius: radius.control, backgroundColor: colors.green500 },
  statusActionClose: { backgroundColor: colors.white, borderWidth: 1, borderColor: colors.lineStrong },
  statusActionText: { color: colors.white, fontFamily: fonts.body, fontSize: 11, fontWeight: '900' },
  statusActionCloseText: { color: colors.ink900 },
  statusActionDisabled: { opacity: 0.45 },
  partialNotice: { minHeight: 44, paddingHorizontal: 12, flexDirection: 'row', alignItems: 'center', gap: 8, borderRadius: radius.control, backgroundColor: colors.apricot50, borderWidth: 1, borderColor: colors.apricot300 },
  partialNoticeText: { flex: 1, color: colors.ink700, fontFamily: fonts.body, fontSize: 11, fontWeight: '700' },
  retryText: { color: colors.ink900, fontFamily: fonts.body, fontSize: 11, fontWeight: '900' },
  priority: { padding: 16, borderRadius: radius.card, backgroundColor: colors.ink900, ...shadow.card },
  priorityHeader: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 12 },
  priorityKicker: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  priorityDot: { width: 6, height: 6, borderRadius: 3, backgroundColor: colors.green300 },
  priorityKickerText: { color: colors.ink400, fontFamily: fonts.body, fontSize: 10, fontWeight: '800' },
  priorityTitle: { marginTop: 5, color: colors.white, fontFamily: fonts.body, fontSize: 20, lineHeight: 27, fontWeight: '900', letterSpacing: -0.55 },
  priorityLink: { minHeight: 44, paddingLeft: 12, flexDirection: 'row', alignItems: 'center', gap: 2 },
  priorityLinkText: { color: colors.white, fontFamily: fonts.body, fontSize: 11, fontWeight: '800' },
  queue: { marginTop: 13, borderTopWidth: StyleSheet.hairlineWidth, borderTopColor: 'rgba(255,255,255,0.14)' },
  queueRow: { minHeight: 65, flexDirection: 'row', alignItems: 'center', gap: 12, borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: 'rgba(255,255,255,0.12)' },
  queueRowPressed: { opacity: 0.68 },
  queueTime: { width: 45 },
  queueTimeValue: { color: colors.white, fontFamily: fonts.body, fontSize: 14, fontWeight: '900', fontVariant: ['tabular-nums'] },
  queueTimeLabel: { marginTop: 2, color: colors.ink400, fontFamily: fonts.body, fontSize: 9 },
  queueCopy: { flex: 1, minWidth: 0 },
  queueDish: { color: colors.white, fontFamily: fonts.body, fontSize: 13, fontWeight: '800' },
  queueMeta: { marginTop: 4, color: colors.ink400, fontFamily: fonts.body, fontSize: 10 },
  queueEmpty: { marginTop: 14, color: colors.ink400, fontFamily: fonts.body, fontSize: 11, lineHeight: 17 },
  insightGrid: { gap: 12 },
  insightGridWide: { flexDirection: 'row', alignItems: 'stretch' },
  revenueHero: { minHeight: 150, padding: 17, justifyContent: 'center', borderRadius: radius.card, backgroundColor: colors.white, borderWidth: 1, borderColor: colors.line },
  revenueHeroWide: { flex: 1.1 },
  revenueLabel: { color: colors.green700, fontFamily: fonts.body, fontSize: 11, fontWeight: '800' },
  revenueValue: { marginTop: 7, color: colors.ink900, fontFamily: fonts.body, fontSize: 31, lineHeight: 39, fontWeight: '900', letterSpacing: -1.1, fontVariant: ['tabular-nums'] },
  revenueUnit: { fontSize: 17, fontWeight: '800' },
  revenueContext: { marginTop: 9, color: colors.ink500, fontFamily: fonts.body, fontSize: 10, lineHeight: 16 },
  supportingMetrics: { minHeight: 118, flexDirection: 'row', alignItems: 'center', borderRadius: radius.card, backgroundColor: colors.white, borderWidth: 1, borderColor: colors.line },
  supportingMetricsWide: { flex: 1 },
  supportingMetric: { flex: 1, alignItems: 'center', justifyContent: 'center', paddingHorizontal: 7 },
  supportingLabel: { color: colors.ink500, fontFamily: fonts.body, fontSize: 10, fontWeight: '700', textAlign: 'center' },
  supportingValue: { marginTop: 6, color: colors.ink900, fontFamily: fonts.body, fontSize: 21, fontWeight: '900', fontVariant: ['tabular-nums'] },
  supportingAlert: { color: colors.danger700 },
  supportingUnit: { fontSize: 10, fontWeight: '700' },
  metricDivider: { width: StyleSheet.hairlineWidth, height: 38, backgroundColor: colors.line },
  sectionHead: { minHeight: 46, flexDirection: 'row', alignItems: 'flex-end', justifyContent: 'space-between', gap: 12 },
  sectionTitle: { color: colors.ink900, fontFamily: fonts.body, fontSize: 18, fontWeight: '900' },
  sectionDescription: { marginTop: 4, color: colors.ink500, fontFamily: fonts.body, fontSize: 10 },
  sectionLinkHit: { minHeight: 44, justifyContent: 'center' },
  sectionLink: { color: colors.green700, fontFamily: fonts.body, fontSize: 11, fontWeight: '800' },
  inventorySurface: { marginTop: 12, overflow: 'hidden', borderRadius: radius.card, backgroundColor: colors.white, borderWidth: 1, borderColor: colors.line },
  inventoryRow: { minHeight: 70, paddingHorizontal: 12, flexDirection: 'row', alignItems: 'center', gap: 11, borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: colors.line },
  inventoryRowLast: { borderBottomWidth: 0 },
  inventoryPressed: { backgroundColor: colors.canvas },
  inventoryImage: { width: 48, height: 48, borderRadius: 10, backgroundColor: colors.canvas },
  inventoryCopy: { flex: 1, minWidth: 0 },
  inventoryName: { color: colors.ink900, fontFamily: fonts.body, fontSize: 13, fontWeight: '800' },
  inventoryStock: { marginTop: 4, color: colors.warning, fontFamily: fonts.body, fontSize: 10, fontWeight: '800' },
  inventorySoldOut: { color: colors.danger700 },
  inventoryAction: { color: colors.ink700, fontFamily: fonts.body, fontSize: 11, fontWeight: '800' },
  inventoryClear: { minHeight: 78, paddingHorizontal: 15, flexDirection: 'row', alignItems: 'center', gap: 11 },
  inventoryClearTitle: { color: colors.ink900, fontFamily: fonts.body, fontSize: 13, fontWeight: '800' },
  inventoryClearBody: { marginTop: 4, color: colors.ink500, fontFamily: fonts.body, fontSize: 10 },
  onboarding: { padding: 24, alignItems: 'center', borderRadius: radius.card, backgroundColor: colors.white, borderWidth: 1, borderColor: colors.line },
  onboardingTitle: { color: colors.ink900, fontFamily: fonts.body, fontSize: 18, fontWeight: '900' },
  onboardingBody: { maxWidth: 360, marginTop: 7, color: colors.ink700, fontFamily: fonts.body, fontSize: 12, lineHeight: 19, textAlign: 'center' },
  pressed: { opacity: 0.68, transform: [{ scale: 0.99 }] },
});
