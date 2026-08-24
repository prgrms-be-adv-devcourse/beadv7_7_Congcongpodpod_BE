import { RoundedIcon as Ionicons } from '@/components/rounded-icon';
import { router, useFocusEffect } from 'expo-router';
import { useCallback, useMemo, useRef, useState } from 'react';
import { FlatList, Pressable, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { OptimizedImage as Image } from '@/components/optimized-image';

import { EmptyState } from '@/components/empty-state';
import { FLOATING_TAB_CONTENT_INSET } from '@/components/floating-tab-bar';
import { AppRefreshControl } from '@/components/app-refresh-control';
import { LoadingState } from '@/components/loading-state';
import { RefreshStatus } from '@/components/refresh-status';
import { colors, fonts, radius, shadow } from '@/constants/theme';
import { useResponsiveLayout } from '@/hooks/use-responsive-layout';
import { usePullToRefresh } from '@/hooks/use-pull-to-refresh';
import { getDishImageSource } from '@/lib/food-image';
import { getMyOrders, getPickupCode, type CustomerOrder, type OrderStatus } from '@/lib/orders';
import { showLoginRequired } from '@/lib/login-required';
import { useAuth } from '@/providers/auth-provider';

const labels: Record<OrderStatus, string> = { RESERVED: '주문 접수', PICKUP_READY: '픽업 대기', PICKED_UP: '픽업 완료', NO_SHOW: '미수령', CANCELLED: '주문 취소', REJECTED: '주문 거절' };
const steps = ['주문 접수', '픽업 대기', '픽업 완료'];
const filters = [['ALL', '전체'], ['ACTIVE', '진행 중'], ['DONE', '완료·취소']] as const;
type Filter = (typeof filters)[number][0];

const progress = (status: OrderStatus) => status === 'PICKED_UP' ? 2 : status === 'PICKUP_READY' ? 1 : 0;
const isStopped = (status: OrderStatus) => ['NO_SHOW', 'CANCELLED', 'REJECTED'].includes(status);
const isActive = (status: OrderStatus) => ['RESERVED', 'PICKUP_READY'].includes(status);
const statusDescriptions: Record<OrderStatus, string> = {
  RESERVED: '매장에서 주문을 확인하고 있어요',
  PICKUP_READY: '픽업 코드를 준비하고 방문해주세요',
  PICKED_UP: '맛있는 한 끼를 구조했어요',
  NO_SHOW: '픽업 시간이 지나 미수령 처리됐어요',
  CANCELLED: '취소가 완료된 주문이에요',
  REJECTED: '매장 사정으로 주문이 거절됐어요',
};

export default function Orders() {
  const { member, initializing } = useAuth();
  const [orders, setOrders] = useState<CustomerOrder[]>([]);
  const [codes, setCodes] = useState<Record<number, string>>({});
  const [filter, setFilter] = useState<Filter>('ALL');
  const [loading, setLoading] = useState(true);
  const loadedOnce = useRef(false);
  const [failed, setFailed] = useState(false);
  const { contentWidth, gutter, isCompact } = useResponsiveLayout();

  const load = useCallback(async (force = false) => {
    if (!loadedOnce.current) setLoading(true);
    setFailed(false);
    try {
      const rows = await getMyOrders(force);
      setOrders(rows);
      const values = await Promise.all(rows.filter((order) => order.status === 'PICKUP_READY').map(async (order) => {
        try { return [order.orderId, (await getPickupCode(order.orderId)).pickupCode] as const; }
        catch { return [order.orderId, ''] as const; }
      }));
      setCodes(Object.fromEntries(values));
    } catch {
      setFailed(true);
    } finally {
      loadedOnce.current = true;
      setLoading(false);
    }
  }, []);

  useFocusEffect(useCallback(() => {
    if (initializing) return;
    if (!member) {
      showLoginRequired('/orders', () => router.replace('/'));
      return;
    }
    void load();
  }, [initializing, load, member]));
  const { refreshing, onRefresh } = usePullToRefresh(() => load(true));

  const visibleOrders = useMemo(() => orders.filter((order) => {
    if (filter === 'ACTIVE') return isActive(order.status);
    if (filter === 'DONE') return !isActive(order.status);
    return true;
  }), [filter, orders]);
  const activeCount = orders.filter((order) => isActive(order.status)).length;

  if (initializing || !member) {
    return <SafeAreaView style={styles.authLoading}><LoadingState compact label="로그인 상태를 확인하고 있어요"/></SafeAreaView>;
  }

  const fixedHeader = <View style={[styles.fixedHeader, { width: contentWidth, paddingHorizontal: gutter }]}><View style={styles.titleRow}><View style={styles.titleCopy}><Text accessibilityRole="header" style={[styles.title, isCompact && styles.titleCompact]}>주문내역</Text><Text style={styles.description}>매장별 준비 상태와 픽업 시간을 확인하세요.</Text></View>{activeCount > 0 ? <View accessibilityLabel={`진행 중인 주문 ${activeCount}건`} style={styles.liveBadge}><View style={styles.liveDot}/><Text style={styles.liveText}>진행 {activeCount}</Text></View> : null}</View><View accessibilityRole="tablist" style={styles.filters}>{filters.map(([key, label]) => <Pressable key={key} accessibilityRole="tab" accessibilityState={{ selected: filter === key }} onPress={() => setFilter(key)} style={styles.filter}><Text style={[styles.filterText, filter === key && styles.filterTextActive]}>{label}</Text>{filter === key ? <View style={styles.filterLine}/> : null}</Pressable>)}</View></View>;
  const header = (
    <View style={[styles.header, { paddingHorizontal: gutter }]}> 
      <View style={styles.listHead}><Text style={styles.listTitle}>{filter === 'ACTIVE' ? '진행 중인 주문' : filter === 'DONE' ? '완료된 주문' : '전체 주문'}</Text><Text style={styles.listCount}>{visibleOrders.length}건</Text></View>
    </View>
  );

  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      {fixedHeader}<RefreshStatus visible={refreshing}/>
      <FlatList
        style={styles.listView}
        alwaysBounceVertical
        data={visibleOrders}
        keyExtractor={(item) => String(item.orderId)}
        ListHeaderComponent={header}
        contentContainerStyle={[styles.list, { width: contentWidth, paddingBottom: FLOATING_TAB_CONTENT_INSET }]}
        renderItem={({ item }) => <View style={{ paddingHorizontal: gutter }}><OrderCard order={item} code={codes[item.orderId]}/></View>}
        ItemSeparatorComponent={() => <View style={styles.separator}/>} 
        refreshControl={<AppRefreshControl refreshing={refreshing} onRefresh={onRefresh}/>}
        showsVerticalScrollIndicator={false}
        ListEmptyComponent={loading ? <LoadingState label="주문 상태를 확인하고 있어요"/> : failed ? <EmptyState title="주문 내역을 불러오지 못했어요" description="네트워크 상태를 확인한 뒤 다시 시도해주세요." actionLabel="다시 불러오기" onAction={() => void load()}/> : <EmptyState title={filter === 'ALL' ? '아직 주문 내역이 없어요' : '이 상태의 주문이 없어요'} description="주변 매장에서 오늘의 마감 할인을 만나보세요." actionLabel="매장 둘러보기" onAction={() => router.push('/stores')}/>} 
      />
    </SafeAreaView>
  );
}

function OrderCard({ order, code }: { order: CustomerOrder; code?: string }) {
  const stopped = isStopped(order.status);
  const terminal = ['PICKED_UP', 'NO_SHOW', 'CANCELLED', 'REJECTED'].includes(order.status);
  const step = progress(order.status);
  const detail = () => router.push(`/orders/${order.orderId}`);

  return (
    <View style={styles.card}>
      <Pressable accessibilityRole="button" accessibilityHint="주문 상세를 엽니다" onPress={detail} style={({ pressed }) => [pressed && styles.pressed]}>
        <View style={styles.cardHead}>
          <View style={styles.storeCopy}><Text numberOfLines={1} style={styles.store}>{order.storeName ?? `매장 #${order.storeId}`}</Text><Text style={styles.orderNumber}>주문 #{order.orderId}</Text></View>
          <View style={[styles.statusBadge, stopped && styles.statusBadgeStopped]}><View style={[styles.statusDot, stopped && styles.statusDotStopped]}/><Text style={[styles.statusText, stopped && styles.statusTextStopped]}>{labels[order.status]}</Text></View>
        </View>
        <Text style={styles.statusDescription}>{statusDescriptions[order.status]}</Text>
        <View style={styles.product}>
          <Image accessibilityLabel={`${order.dishName} 상품 이미지`} source={getDishImageSource({ imageUrl: order.storeImageUrl })} style={styles.photo}/>
          <View style={styles.productCopy}><Text numberOfLines={1} style={styles.menu}>{order.dishName} × {order.quantity}</Text><Text style={styles.pickup}>{order.pickupStartAt?.slice(0, 5) ?? '--:--'}–{order.pickupEndAt?.slice(0, 5) ?? '--:--'} 픽업</Text><Text style={styles.price}>{Number(order.totalPrice).toLocaleString()}원</Text></View>
          <View style={styles.detailLink}><Text style={styles.detailText}>상세</Text><Ionicons name="chevron-forward" size={15} color={colors.ink700}/></View>
        </View>
        {!stopped ? <View style={styles.sequence}>{steps.map((label, index) => <View key={label} style={styles.step}>{index ? <View style={[styles.connector, index <= step && styles.doneBg]}/> : null}<View style={[styles.dot, index <= step && styles.doneBg]}>{index <= step ? <Ionicons name="checkmark" size={9} color={colors.white}/> : null}</View><Text style={[styles.stepText, index <= step && styles.doneText]}>{label}</Text></View>)}</View> : <View style={styles.stopped}><Ionicons name="information-circle-outline" size={16} color={colors.ink700}/><Text style={styles.stoppedText}>{labels[order.status]} 처리된 주문입니다.</Text></View>}
      </Pressable>
      {order.status === 'PICKUP_READY' && code ? <View style={styles.pickupCode}><View><Text style={styles.codeLabel}>매장에 보여줄 픽업 코드</Text><Text style={styles.code}>{code}</Text></View><Ionicons name="qr-code-outline" size={24} color={colors.white}/></View> : terminal && order.status === 'PICKED_UP' ? <Pressable accessibilityRole="button" onPress={() => undefined} style={({ pressed }) => [styles.review, pressed && styles.pressed]}><Ionicons name="create-outline" size={17} color={colors.ink900}/><Text style={styles.reviewText}>리뷰 작성하기</Text></Pressable> : null}
    </View>
  );
}

const styles = StyleSheet.create({
  authLoading: { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.white },
  safe: { flex: 1, backgroundColor: colors.white },
  listView: { flex: 1 },
  list: { alignSelf: 'center', flexGrow: 1 },
  fixedHeader: { alignSelf: 'center', paddingTop: 20, backgroundColor: colors.white }, header: { paddingBottom: 12 },
  titleRow: { flexDirection: 'row', alignItems: 'flex-start', justifyContent: 'space-between', gap: 14 },
  titleCopy: { flex: 1 },
  title: { color: colors.ink900, fontFamily: fonts.body, fontSize: 28, lineHeight: 35, fontWeight: '900', letterSpacing: -1.1 },
  titleCompact: { fontSize: 25, lineHeight: 32 },
  description: { marginTop: 6, color: colors.ink700, fontFamily: fonts.body, fontSize: 13, lineHeight: 20 },
  liveBadge: { minHeight: 30, paddingHorizontal: 10, flexDirection: 'row', alignItems: 'center', gap: 6, borderRadius: radius.pill, backgroundColor: colors.green50 },
  liveDot: { width: 6, height: 6, borderRadius: 3, backgroundColor: colors.green500 },
  liveText: { color: colors.green700, fontFamily: fonts.body, fontSize: 11, fontWeight: '800' },
  filters: { marginTop: 19, flexDirection: 'row', borderBottomWidth: 1, borderBottomColor: colors.line },
  filter: { flex: 1, minHeight: 48, alignItems: 'center', justifyContent: 'center' },
  filterText: { color: colors.ink400, fontFamily: fonts.body, fontSize: 13, fontWeight: '700' },
  filterTextActive: { color: colors.ink900, fontWeight: '900' },
  filterLine: { position: 'absolute', left: '50%', bottom: 0, width: 48, height: 3, marginLeft: -24, borderRadius: 2, backgroundColor: colors.ink900 },
  listHead: { marginTop: 21, paddingBottom: 3, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  listTitle: { color: colors.ink900, fontFamily: fonts.body, fontSize: 18, fontWeight: '900' },
  listCount: { color: colors.ink500, fontFamily: fonts.body, fontSize: 11, fontWeight: '700' },
  separator: { height: 11 },
  card: { padding: 16, borderRadius: radius.card, borderWidth: 1, borderColor: colors.line, backgroundColor: colors.white, ...shadow.card },
  pressed: { opacity: 0.72 },
  cardHead: { flexDirection: 'row', alignItems: 'flex-start', justifyContent: 'space-between', gap: 12 },
  storeCopy: { flex: 1 },
  store: { color: colors.ink900, fontFamily: fonts.body, fontSize: 17, fontWeight: '900', letterSpacing: -0.4 },
  orderNumber: { marginTop: 4, color: colors.ink400, fontFamily: fonts.body, fontSize: 10 },
  statusBadge: { minHeight: 28, flexDirection: 'row', alignItems: 'center', gap: 6 },
  statusBadgeStopped: {},
  statusDot: { width: 6, height: 6, borderRadius: 3, backgroundColor: colors.green500 },
  statusDotStopped: { backgroundColor: colors.ink400 },
  statusText: { color: colors.green700, fontFamily: fonts.body, fontSize: 10, fontWeight: '800' },
  statusTextStopped: { color: colors.ink700 },
  statusDescription: { marginTop: 8, color: colors.ink700, fontFamily: fonts.body, fontSize: 12, lineHeight: 18 },
  product: { marginTop: 13, paddingVertical: 13, flexDirection: 'row', alignItems: 'center', gap: 11, borderTopWidth: StyleSheet.hairlineWidth, borderColor: colors.line },
  photo: { width: 64, height: 64, borderRadius: radius.control, backgroundColor: colors.canvas },
  productCopy: { flex: 1 },
  menu: { color: colors.ink900, fontFamily: fonts.body, fontSize: 14, fontWeight: '800' },
  pickup: { marginTop: 5, color: colors.ink500, fontFamily: fonts.body, fontSize: 10 },
  price: { marginTop: 5, color: colors.ink900, fontFamily: fonts.body, fontSize: 13, fontWeight: '900' },
  detailLink: { minWidth: 44, minHeight: 44, flexDirection: 'row', alignItems: 'center', justifyContent: 'flex-end', gap: 1 },
  detailText: { color: colors.ink700, fontFamily: fonts.body, fontSize: 10, fontWeight: '700' },
  sequence: { flexDirection: 'row', marginTop: 16, marginBottom: 3, paddingTop: 7, paddingBottom: 5 },
  step: { flex: 1, alignItems: 'center', paddingTop: 17 },
  dot: { position: 'absolute', top: 0, zIndex: 2, width: 13, height: 13, borderRadius: 7, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.line },
  connector: { position: 'absolute', right: '50%', top: 5, width: '100%', height: 3, backgroundColor: colors.line },
  stepText: { color: colors.ink400, fontFamily: fonts.body, fontSize: 9, fontWeight: '600' },
  doneBg: { backgroundColor: colors.green500 },
  doneText: { color: colors.green700, fontWeight: '800' },
  stopped: { minHeight: 42, marginTop: 12, paddingTop: 11, flexDirection: 'row', alignItems: 'center', gap: 7, borderTopWidth: StyleSheet.hairlineWidth, borderTopColor: colors.line },
  stoppedText: { color: colors.ink700, fontFamily: fonts.body, fontSize: 11 },
  pickupCode: { minHeight: 66, marginTop: 13, paddingHorizontal: 14, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', borderRadius: 10, backgroundColor: colors.ink900 },
  codeLabel: { color: colors.ink400, fontFamily: fonts.body, fontSize: 10 },
  code: { marginTop: 4, color: colors.white, fontFamily: fonts.body, fontSize: 20, letterSpacing: 2, fontWeight: '900' },
  review: { minHeight: 44, marginTop: 13, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 6, borderRadius: 9, borderWidth: 1, borderColor: colors.lineStrong, backgroundColor: colors.white },
  reviewText: { color: colors.ink900, fontFamily: fonts.body, fontSize: 12, fontWeight: '800' },
});
