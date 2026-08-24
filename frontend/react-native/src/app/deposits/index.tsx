import { RoundedIcon as Ionicons } from '@/components/rounded-icon';
import { router, useFocusEffect } from 'expo-router';
import { useCallback, useMemo, useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { EmptyState } from '@/components/empty-state';
import { LoadingState } from '@/components/loading-state';
import { Page } from '@/components/page';
import { Pagination } from '@/components/pagination';
import { colors, fonts, radius, shadow } from '@/constants/theme';
import { getDepositBalance, getDepositHistory, type DepositHistory } from '@/lib/account';
import { usePullToRefresh } from '@/hooks/use-pull-to-refresh';

const typeLabel = { CHARGE: '예치금 충전', USE: '주문 결제', REFUND: '주문 취소 환불' } as const;
const typeIcon: Record<DepositHistory['type'], keyof typeof Ionicons.glyphMap> = { CHARGE: 'add', USE: 'receipt-outline', REFUND: 'return-down-back-outline' };

export default function Deposits() {
  const [balance, setBalance] = useState(0);
  const [history, setHistory] = useState<DepositHistory[]>([]);
  const [loading, setLoading] = useState(true);
  const [failed, setFailed] = useState(false);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  const load = useCallback(async () => {
    setLoading(true);
    setFailed(false);
    try {
      const [nextBalance, nextHistory] = await Promise.all([getDepositBalance(), getDepositHistory(page, 7)]);
      setBalance(nextBalance);
      setHistory(nextHistory.content);
      setTotalPages(nextHistory.totalPages);
      setTotalElements(nextHistory.totalElements);
    } catch {
      setFailed(true);
    } finally {
      setLoading(false);
    }
  }, [page]);

  useFocusEffect(useCallback(() => { void load(); }, [load]));
  const { refreshing, onRefresh } = usePullToRefresh(load);

  const groups = useMemo(() => history.reduce<Record<string, DepositHistory[]>>((acc, row) => {
    const date = new Date(row.createdAt);
    const key = new Intl.DateTimeFormat('ko-KR', { month: 'long', day: 'numeric', weekday: 'short' }).format(date);
    (acc[key] ??= []).push(row);
    return acc;
  }, {}), [history]);

  return (
    <Page title="예치금" description="충전부터 주문 결제와 환불까지 한눈에 확인하세요." refreshing={refreshing} onRefresh={onRefresh}>
      <View style={styles.wallet}>
        <View style={styles.walletTop}><View style={styles.walletBrand}><View style={styles.walletIcon}><Ionicons name="wallet-outline" size={18} color={colors.green300}/></View><Text style={styles.pay}>라디페이</Text></View><Text style={styles.secure}>안전하게 보관 중</Text></View>
        <Text style={styles.walletLabel}>사용 가능한 잔액</Text>
        <Text style={styles.balance}>{loading && !history.length ? '—' : `${balance.toLocaleString()}원`}</Text>
        <View style={styles.walletBottom}><Text style={styles.walletHint}>주문할 때 예치금으로 바로 결제할 수 있어요.</Text><Pressable accessibilityRole="button" onPress={() => router.push('/deposits/charge')} style={({ pressed }) => [styles.charge, pressed && styles.pressed]}><Ionicons name="add" size={17} color={colors.ink900}/><Text style={styles.chargeText}>충전</Text></Pressable></View>
      </View>

      <View style={styles.historyHead}><View><Text style={styles.heading}>이용내역</Text><Text style={styles.historyDescription}>최신 내역부터 페이지당 7개씩 표시해요.</Text></View><Text style={styles.count}>{totalElements}건</Text></View>

      {loading && !history.length ? <LoadingState label="예치금 내역을 확인하고 있어요" compact/> : failed && !history.length ? <EmptyState title="예치금 내역을 불러오지 못했어요" description="잠시 후 다시 확인해주세요." actionLabel="다시 불러오기" onAction={() => void load()}/> : history.length === 0 ? <EmptyState title="아직 예치금 내역이 없어요" description="충전하거나 주문을 결제하면 여기에 기록돼요."/> : <><View style={styles.history}>{Object.entries(groups).map(([date, rows]) => <View key={date} style={styles.group}><Text style={styles.day}>{date}</Text><View style={styles.groupSurface}>{rows.map((row) => <HistoryRow key={row.id} row={row}/>)}</View></View>)}</View><Pagination page={page} totalPages={totalPages} onChange={setPage}/></>}
    </Page>
  );
}

function HistoryRow({ row }: { row: DepositHistory }) {
  const plus = row.type !== 'USE';
  const time = new Date(row.createdAt).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit', hour12: false });
  return <View style={styles.row}><View style={[styles.typeIcon, plus && styles.typeIconPlus]}><Ionicons name={typeIcon[row.type]} size={17} color={plus ? colors.green700 : colors.ink700}/></View><View style={styles.rowCopy}><Text style={styles.name}>{typeLabel[row.type]}</Text><Text style={styles.meta}>{row.orderId ? `주문 #${row.orderId}` : `결제 #${row.paymentId ?? '-'}`} · {time}</Text></View><View style={styles.amountCopy}><Text style={[styles.amount, plus && styles.plus]}>{plus ? '+' : '−'}{Math.abs(Number(row.amount)).toLocaleString()}원</Text><Text style={styles.after}>{Number(row.balanceAfter).toLocaleString()}원 남음</Text></View></View>;
}

const styles = StyleSheet.create({
  wallet: { padding: 18, borderRadius: radius.card, backgroundColor: colors.ink900, ...shadow.card },
  walletTop: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  walletBrand: { flexDirection: 'row', alignItems: 'center', gap: 7 },
  walletIcon: { width: 32, height: 32, alignItems: 'center', justifyContent: 'center', borderRadius: 10, backgroundColor: 'rgba(255,255,255,0.09)' },
  pay: { color: colors.white, fontFamily: fonts.body, fontSize: 12, fontWeight: '900' },
  secure: { color: colors.ink400, fontFamily: fonts.body, fontSize: 9, fontWeight: '600' },
  walletLabel: { marginTop: 19, color: colors.ink400, fontFamily: fonts.body, fontSize: 11, fontWeight: '700' },
  balance: { marginTop: 4, color: colors.white, fontFamily: fonts.body, fontSize: 31, fontWeight: '900', letterSpacing: -1.2 },
  walletBottom: { marginTop: 18, flexDirection: 'row', alignItems: 'center', gap: 12 },
  walletHint: { flex: 1, color: colors.ink400, fontFamily: fonts.body, fontSize: 10, lineHeight: 15 },
  charge: { minWidth: 76, minHeight: 44, paddingHorizontal: 13, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 4, borderRadius: 9, backgroundColor: colors.white },
  chargeText: { color: colors.ink900, fontFamily: fonts.body, fontSize: 12, fontWeight: '900' },
  pressed: { opacity: 0.7, transform: [{ scale: 0.98 }] },
  historyHead: { marginTop: 12, paddingTop: 8, flexDirection: 'row', alignItems: 'flex-end', justifyContent: 'space-between' },
  heading: { color: colors.ink900, fontFamily: fonts.body, fontSize: 19, fontWeight: '900', letterSpacing: -0.5 },
  historyDescription: { marginTop: 4, color: colors.ink500, fontFamily: fonts.body, fontSize: 10 },
  count: { color: colors.ink500, fontFamily: fonts.body, fontSize: 11, fontWeight: '700' },
  history: { gap: 3 },
  group: { marginTop: 5 },
  day: { paddingVertical: 8, color: colors.ink500, fontFamily: fonts.body, fontSize: 11, fontWeight: '700' },
  groupSurface: { paddingHorizontal: 13, overflow: 'hidden', borderRadius: 12, backgroundColor: colors.white, borderWidth: 1, borderColor: colors.line },
  row: { minHeight: 76, flexDirection: 'row', alignItems: 'center', gap: 10, borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: colors.line },
  typeIcon: { width: 38, height: 38, alignItems: 'center', justifyContent: 'center', borderRadius: 11, backgroundColor: colors.canvas },
  typeIconPlus: { backgroundColor: colors.green50 },
  rowCopy: { flex: 1 },
  name: { color: colors.ink900, fontFamily: fonts.body, fontSize: 13, fontWeight: '800' },
  meta: { marginTop: 4, color: colors.ink500, fontFamily: fonts.body, fontSize: 10 },
  amountCopy: { alignItems: 'flex-end' },
  amount: { color: colors.ink900, fontFamily: fonts.body, fontSize: 13, fontWeight: '900' },
  plus: { color: colors.green700 },
  after: { marginTop: 4, color: colors.ink400, fontFamily: fonts.body, fontSize: 9 },
});
