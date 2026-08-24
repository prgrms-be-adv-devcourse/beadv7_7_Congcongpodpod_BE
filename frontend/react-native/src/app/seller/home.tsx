import { RoundedIcon as Ionicons } from '@/components/rounded-icon';
import { router, useFocusEffect } from 'expo-router';
import { useCallback, useState } from 'react';
import { Image, Pressable, StyleSheet, Text, View } from 'react-native';

import { EmptyState } from '@/components/empty-state';
import { ConfirmModal } from '@/components/confirm-modal';
import { LoadingState } from '@/components/loading-state';
import { showAppAlert } from '@/lib/app-overlay';
import { PrimaryButton } from '@/components/page';
import { SellerShell } from '@/components/seller-shell';
import { colors, fonts, radius, shadow } from '@/constants/theme';
import { getStoreProfileImageSource } from '@/lib/food-image';
import { changeStoreStatus, getMyStores, getSellerDishes, getSettlements, getStoreOrders, getStoreSales, type Settlement } from '@/lib/seller';
import { getStoreCategoryVisual } from '@/lib/store-category';
import { usePullToRefresh } from '@/hooks/use-pull-to-refresh';
import type { Store } from '@/types/store';

export default function SellerHome() {
  const [store, setStore] = useState<Store | null>(null);
  const [orderCount, setOrderCount] = useState(0);
  const [readyCount, setReadyCount] = useState(0);
  const [dishCount, setDishCount] = useState(0);
  const [todaySales, setTodaySales] = useState(0);
  const [latestSettlement, setLatestSettlement] = useState<Settlement | null>(null);
  const [salesLabel, setSalesLabel] = useState('오늘 매출');
  const [loading, setLoading] = useState(true);
  const [failed, setFailed] = useState(false);
  const [statusChanging, setStatusChanging] = useState(false);
  const [pendingStatus, setPendingStatus] = useState<'OPEN' | 'CLOSED'>();

  const load = useCallback(async () => {
    setLoading(true);
    setFailed(false);
    try {
      const [mine] = await getMyStores();
      if (!mine) { setStore(null); return; }
      setStore(mine);
      const [orders, dishes, sales, settlements] = await Promise.all([
        getStoreOrders(mine.storeId),
        getSellerDishes(mine.storeId),
        getStoreSales(mine.storeId).catch(() => null),
        getSettlements().catch(() => []),
      ]);
      setOrderCount(orders.filter((order) => order.status === 'RESERVED').length);
      setReadyCount(orders.filter((order) => order.status === 'PICKUP_READY').length);
      setDishCount(dishes.length);
      const recentSales = orders.filter((order) => !['CANCELLED', 'REJECTED'].includes(order.status)).reduce((sum, order) => sum + Number(order.totalPrice), 0);
      setTodaySales(sales?.salesAmount ?? recentSales);
      setSalesLabel(sales ? '오늘 매출' : '최근 매출');
      setLatestSettlement(settlements[0] ?? null);
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

  if (loading && !store) return <SellerShell title="대시보드" description="오늘 운영 현황을 준비하고 있어요." storeName="내 매장"><LoadingState compact label="매장 현황을 불러오고 있어요"/></SellerShell>;
  if (failed && !store) return <SellerShell title="대시보드" description="매장 운영 정보를 불러오지 못했어요." storeName="내 매장" refreshing={refreshing} onRefresh={onRefresh}><EmptyState title="대시보드를 열지 못했어요" description="네트워크 상태를 확인한 뒤 다시 시도해주세요." actionLabel="다시 불러오기" onAction={() => void load()}/></SellerShell>;
  if (!store) return <SellerShell title="판매를 시작해볼까요?" description="상점을 등록하면 상품·주문·정산을 한곳에서 관리할 수 있어요." storeName="미등록" refreshing={refreshing} onRefresh={onRefresh}><View style={styles.onboarding}><View style={styles.onboardingIcon}><Ionicons name="storefront-outline" size={27} color={colors.green700}/></View><Text style={styles.onboardingTitle}>첫 매장을 등록해주세요</Text><Text style={styles.onboardingBody}>사업자 정보와 매장 위치를 입력하면 판매자 권한을 확인한 뒤 대시보드가 열려요.</Text></View><PrimaryButton label="상점 등록하기" onPress={() => router.push('/seller/store')}/></SellerShell>;

  const taskCount = orderCount + readyCount;
  const settlementMeta = latestSettlement
    ? `${latestSettlement.settlementMonth.replace('-', '.')} · ${settlementStatusLabel(latestSettlement.status)} · ${latestSettlement.settlementAmount.toLocaleString()}원`
    : '아직 생성된 정산 내역이 없어요';
  return <SellerShell title="대시보드" description="놓치면 안 되는 주문과 오늘 매출을 먼저 보여드려요." storeName={store.storeName} storeStatus={store.status} refreshing={refreshing} onRefresh={onRefresh}>
    <StoreSummary store={store} statusChanging={statusChanging} onManage={() => router.push('/seller/store')} onStatusChange={() => setPendingStatus(store.status === 'OPEN' ? 'CLOSED' : 'OPEN')}/>
    <View style={styles.priority}>
      <View style={styles.priorityTop}><View style={styles.priorityBadge}><View style={styles.priorityDot}/><Text style={styles.priorityBadgeText}>오늘 할 일</Text></View><Ionicons name="notifications-outline" size={20} color={colors.green300}/></View>
      <Text style={styles.priorityTitle}>{taskCount ? `${taskCount}건의 주문 확인이 필요해요` : '처리할 주문이 모두 정리됐어요'}</Text>
      <View style={styles.priorityStats}><PriorityStat label="접수 대기" value={orderCount}/><View style={styles.priorityDivider}/><PriorityStat label="픽업 대기" value={readyCount}/></View>
      <Pressable accessibilityRole="button" onPress={() => router.push('/seller/orders')} style={({ pressed }) => [styles.priorityAction, pressed && styles.pressed]}><Text style={styles.priorityActionText}>주문 관리 열기</Text><Ionicons name="arrow-forward" size={17} color={colors.ink900}/></Pressable>
    </View>

    <View style={styles.metrics}>
      <Metric icon="cash-outline" label={salesLabel} value={`${todaySales.toLocaleString()}원`}/>
      <Metric icon="restaurant-outline" label="등록 상품" value={`${dishCount}개`}/>
    </View>

    <View style={styles.sectionHead}><View><Text style={styles.heading}>운영 메뉴</Text><Text style={styles.sectionDescription}>자주 쓰는 업무로 바로 이동하세요.</Text></View></View>
    <View style={styles.tasks}>
      <TaskRow icon="receipt-outline" title="주문 처리" meta={`접수 ${orderCount}건 · 픽업 ${readyCount}건`} onPress={() => router.push('/seller/orders')}/>
      <TaskRow icon="fast-food-outline" title="상품 운영" meta={`현재 ${dishCount}개 상품 등록`} onPress={() => router.push('/seller/dishes')}/>
      <TaskRow icon="wallet-outline" title="정산 확인" meta={settlementMeta} onPress={() => router.push('/seller/settlements')} last/>
    </View>
    <ConfirmModal visible={Boolean(pendingStatus)} icon={pendingStatus === 'OPEN' ? 'storefront-outline' : 'moon-outline'} title={pendingStatus === 'OPEN' ? '매장을 열까요?' : '매장을 닫을까요?'} description={pendingStatus === 'OPEN' ? '변경 즉시 고객의 지도와 목록에 매장이 노출됩니다.' : '변경 즉시 고객의 지도와 목록에서 매장이 숨겨집니다.'} confirmLabel={pendingStatus === 'OPEN' ? '매장 열기' : '매장 닫기'} busy={statusChanging} onCancel={() => setPendingStatus(undefined)} onConfirm={() => void applyStatusChange()}/>
  </SellerShell>;
}

function StoreSummary({ store, statusChanging, onManage, onStatusChange }: { store: Store; statusChanging: boolean; onManage: () => void; onStatusChange: () => void }) {
  const category = getStoreCategoryVisual(store.category);
  const statusLabel = store.status === 'OPEN' ? '운영 중' : store.status === 'STOPPED' ? '운영 중지' : '오픈 전';
  const hours = store.openTime && store.closeTime ? `${store.openTime.slice(0, 5)}–${store.closeTime.slice(0, 5)}` : '영업시간 미등록';
  return <View style={styles.operation}>
    <View style={styles.storeSummaryTop}>
      <Image source={getStoreProfileImageSource(store)} style={styles.storeProfile}/>
      <View style={styles.storeSummaryCopy}>
        <View style={styles.storeNameRow}><Text numberOfLines={1} style={styles.storeSummaryName}>{store.storeName}</Text><Text style={styles.storeCategory}>{category.label}</Text></View>
        <View style={styles.storeMetaRow}><Ionicons name="location-outline" size={13} color={colors.ink500}/><Text numberOfLines={1} style={styles.storeMeta}>{store.address || '주소 미등록'}</Text></View>
        <View style={styles.storeMetaRow}><Ionicons name="time-outline" size={13} color={colors.ink500}/><Text style={styles.storeMeta}>{hours}</Text></View>
      </View>
      <Pressable accessibilityRole="button" accessibilityLabel="매장 정보 관리" onPress={onManage} style={({ pressed }) => [styles.manageAction, pressed && styles.pressed]}><Text style={styles.manageText}>관리</Text><Ionicons name="chevron-forward" size={16} color={colors.ink700}/></Pressable>
    </View>
    <View style={styles.operationBottom}>
      <View style={styles.operationCopy}><View style={styles.operationTitleRow}><View style={[styles.operationDot, store.status !== 'OPEN' && styles.operationDotClosed]}/><Text style={styles.operationTitle}>{statusLabel}</Text></View><Text style={styles.operationDescription}>{store.status === 'OPEN' ? '고객이 매장과 판매 상품을 확인할 수 있어요.' : store.status === 'STOPPED' ? '관리자에 의해 운영이 중지된 매장이에요.' : '준비가 끝나면 매장을 열어주세요.'}</Text></View>
      <Pressable accessibilityRole="button" disabled={statusChanging || store.status === 'STOPPED'} onPress={onStatusChange} style={({ pressed }) => [styles.statusAction, store.status === 'OPEN' && styles.statusActionClose, (pressed || statusChanging || store.status === 'STOPPED') && styles.statusActionDisabled]}><Text style={[styles.statusActionText, store.status === 'OPEN' && styles.statusActionCloseText]}>{store.status === 'OPEN' ? '매장 닫기' : store.status === 'STOPPED' ? '변경 불가' : '매장 열기'}</Text></Pressable>
    </View>
  </View>;
}

function settlementStatusLabel(status: string) {
  if (status === 'COMPLETED') return '지급 완료';
  if (status === 'FAILED') return '정산 실패';
  return '처리 중';
}

function PriorityStat({ label, value }: { label: string; value: number }) {
  return <View style={styles.priorityStat}><Text style={styles.priorityLabel}>{label}</Text><Text style={styles.priorityValue}>{value}<Text style={styles.priorityUnit}>건</Text></Text></View>;
}

function Metric({ icon, label, value }: { icon: keyof typeof Ionicons.glyphMap; label: string; value: string }) {
  return <View style={styles.metric}><View style={styles.metricIcon}><Ionicons name={icon} size={18} color={colors.ink700}/></View><Text style={styles.metricLabel}>{label}</Text><Text numberOfLines={1} adjustsFontSizeToFit style={styles.metricValue}>{value}</Text></View>;
}

function TaskRow({ icon, title, meta, onPress, last }: { icon: keyof typeof Ionicons.glyphMap; title: string; meta: string; onPress: () => void; last?: boolean }) {
  return <Pressable accessibilityRole="button" onPress={onPress} style={({ pressed }) => [styles.row, last && styles.rowLast, pressed && styles.pressed]}><View style={styles.taskIcon}><Ionicons name={icon} size={19} color={colors.ink700}/></View><View style={styles.rowCopy}><Text style={styles.name}>{title}</Text><Text style={styles.meta}>{meta}</Text></View><Ionicons name="chevron-forward" size={18} color={colors.ink400}/></Pressable>;
}

const styles = StyleSheet.create({
  operation: { overflow: 'hidden', borderRadius: radius.card, backgroundColor: colors.white, borderWidth: 1, borderColor: colors.line, ...shadow.card },
  storeSummaryTop: { minHeight: 94, padding: 13, flexDirection: 'row', alignItems: 'center', gap: 11 },
  storeProfile: { width: 58, height: 58, borderRadius: 15, backgroundColor: colors.canvas },
  storeSummaryCopy: { flex: 1, minWidth: 0 },
  storeNameRow: { flexDirection: 'row', alignItems: 'center', gap: 7 },
  storeSummaryName: { flexShrink: 1, color: colors.ink900, fontFamily: fonts.body, fontSize: 15, fontWeight: '900' },
  storeCategory: { color: colors.ink500, fontFamily: fonts.body, fontSize: 9, fontWeight: '700' },
  storeMetaRow: { marginTop: 5, flexDirection: 'row', alignItems: 'center', gap: 4 },
  storeMeta: { flexShrink: 1, color: colors.ink500, fontFamily: fonts.body, fontSize: 10 },
  manageAction: { minWidth: 62, minHeight: 44, paddingHorizontal: 10, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 2, borderRadius: radius.control, backgroundColor: colors.canvas },
  manageText: { color: colors.ink900, fontFamily: fonts.body, fontSize: 11, fontWeight: '900' },
  operationBottom: { minHeight: 78, paddingHorizontal: 13, paddingVertical: 11, flexDirection: 'row', alignItems: 'center', gap: 12, borderTopWidth: 1, borderTopColor: colors.line, backgroundColor: colors.canvasWarm },
  operationCopy: { flex: 1, minWidth: 0 },
  operationTitleRow: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  operationDot: { width: 7, height: 7, borderRadius: 4, backgroundColor: colors.green500 },
  operationDotClosed: { backgroundColor: colors.ink400 },
  operationTitle: { color: colors.ink900, fontFamily: fonts.body, fontSize: 13, fontWeight: '900' },
  operationDescription: { maxWidth: 330, marginTop: 5, color: colors.ink500, fontFamily: fonts.body, fontSize: 9, lineHeight: 14 },
  statusAction: { minWidth: 86, minHeight: 44, paddingHorizontal: 13, alignItems: 'center', justifyContent: 'center', borderRadius: radius.control, backgroundColor: colors.green500 },
  statusActionClose: { backgroundColor: colors.canvas, borderWidth: 1, borderColor: colors.lineStrong },
  statusActionText: { color: colors.white, fontFamily: fonts.body, fontSize: 11, fontWeight: '900' },
  statusActionCloseText: { color: colors.ink900 },
  statusActionDisabled: { opacity: 0.45 },
  priority: { padding: 17, borderRadius: radius.card, backgroundColor: colors.ink900, ...shadow.card },
  priorityTop: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  priorityBadge: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  priorityDot: { width: 6, height: 6, borderRadius: 3, backgroundColor: colors.green300 },
  priorityBadgeText: { color: colors.ink400, fontFamily: fonts.body, fontSize: 10, fontWeight: '800' },
  priorityTitle: { maxWidth: 330, marginTop: 12, color: colors.white, fontFamily: fonts.body, fontSize: 21, lineHeight: 28, fontWeight: '900', letterSpacing: -0.65 },
  priorityStats: { marginTop: 18, flexDirection: 'row', alignItems: 'center' },
  priorityStat: { flex: 1 },
  priorityDivider: { width: 1, height: 34, marginHorizontal: 16, backgroundColor: 'rgba(255,255,255,0.13)' },
  priorityLabel: { color: colors.ink400, fontFamily: fonts.body, fontSize: 10 },
  priorityValue: { marginTop: 3, color: colors.white, fontFamily: fonts.body, fontSize: 24, fontWeight: '900' },
  priorityUnit: { fontSize: 12, fontWeight: '700' },
  priorityAction: { minHeight: 46, marginTop: 17, paddingHorizontal: 14, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', borderRadius: radius.control, backgroundColor: colors.white },
  priorityActionText: { color: colors.ink900, fontFamily: fonts.body, fontSize: 12, fontWeight: '900' },
  metrics: { flexDirection: 'row', gap: 10 },
  metric: { flex: 1, minHeight: 112, padding: 13, borderRadius: radius.card, backgroundColor: colors.white, borderWidth: 1, borderColor: colors.line, ...shadow.card },
  metricIcon: { width: 34, height: 34, alignItems: 'center', justifyContent: 'center', borderRadius: 10, backgroundColor: colors.canvas },
  metricLabel: { marginTop: 11, color: colors.ink500, fontFamily: fonts.body, fontSize: 10, fontWeight: '700' },
  metricValue: { marginTop: 3, color: colors.ink900, fontFamily: fonts.body, fontSize: 19, fontWeight: '900', letterSpacing: -0.5 },
  sectionHead: { marginTop: 7, flexDirection: 'row', alignItems: 'flex-end', justifyContent: 'space-between' },
  heading: { color: colors.ink900, fontFamily: fonts.body, fontSize: 18, fontWeight: '900' },
  sectionDescription: { marginTop: 4, color: colors.ink500, fontFamily: fonts.body, fontSize: 10 },
  link: { color: colors.green700, fontFamily: fonts.body, fontSize: 11, fontWeight: '800' },
  tasks: { paddingHorizontal: 13, overflow: 'hidden', borderRadius: 12, backgroundColor: colors.white, borderWidth: 1, borderColor: colors.line },
  row: { minHeight: 72, flexDirection: 'row', alignItems: 'center', gap: 11, borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: colors.line },
  rowLast: { borderBottomWidth: 0 },
  taskIcon: { width: 38, height: 38, alignItems: 'center', justifyContent: 'center', borderRadius: 11, backgroundColor: colors.canvas },
  rowCopy: { flex: 1 },
  name: { color: colors.ink900, fontFamily: fonts.body, fontSize: 14, fontWeight: '900' },
  meta: { marginTop: 4, color: colors.ink500, fontFamily: fonts.body, fontSize: 10 },
  pressed: { opacity: 0.68, transform: [{ scale: 0.99 }] },
  onboarding: { padding: 24, alignItems: 'center', borderRadius: radius.card, backgroundColor: colors.white, borderWidth: 1, borderColor: colors.line },
  onboardingIcon: { width: 58, height: 58, alignItems: 'center', justifyContent: 'center', borderRadius: 18, backgroundColor: colors.green50 },
  onboardingTitle: { marginTop: 13, color: colors.ink900, fontFamily: fonts.body, fontSize: 18, fontWeight: '900' },
  onboardingBody: { maxWidth: 360, marginTop: 7, color: colors.ink700, fontFamily: fonts.body, fontSize: 12, lineHeight: 19, textAlign: 'center' },
});
