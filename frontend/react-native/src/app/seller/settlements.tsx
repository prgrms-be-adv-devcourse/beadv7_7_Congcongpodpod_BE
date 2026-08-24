import { RoundedIcon as Ionicons } from '@/components/rounded-icon';
import { useCallback, useEffect, useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { EmptyState } from '@/components/empty-state';
import { LoadingState } from '@/components/loading-state';
import { Panel, Row } from '@/components/page';
import { SellerShell } from '@/components/seller-shell';
import { colors, fonts, radius } from '@/constants/theme';
import { getMyStores, getSettlements, type Settlement } from '@/lib/seller';
import { usePullToRefresh } from '@/hooks/use-pull-to-refresh';

export default function Settlements() {
  const [items, setItems] = useState<Settlement[]>([]);
  const [index, setIndex] = useState(0);
  const [storeName, setStoreName] = useState('내 매장');
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [stores, settlements] = await Promise.all([getMyStores(), getSettlements()]);
      setStoreName(stores[0]?.storeName ?? '미등록');
      const id = stores[0]?.storeId;
      setItems(id ? settlements.filter((item) => item.storeId === id) : []);
      setIndex(0);
    } finally {
      setLoading(false);
    }
  }, []);
  useEffect(() => { void load(); }, [load]);
  const { refreshing, onRefresh } = usePullToRefresh(load);

  const current = items[index];
  return (
    <SellerShell title="정산" description="매장별 월 정산을 한눈에 확인하세요." storeName={storeName} refreshing={refreshing} onRefresh={onRefresh}>
      {loading && !items.length ? <LoadingState label="정산 내역을 계산하고 있어요" compact/> : !current ? (
        <EmptyState title="정산 내역이 없어요" description="판매가 시작되면 월별 정산 금액을 확인할 수 있어요."/>
      ) : <>
        <View style={styles.month}>
          <Pressable accessibilityLabel="이전 정산 월" disabled={index >= items.length - 1} onPress={() => setIndex(index + 1)} style={styles.monthButton}><Ionicons name="chevron-back" size={18} color={index >= items.length - 1 ? colors.ink400 : colors.ink900}/></Pressable>
          <View style={styles.monthCopy}><Text style={styles.monthText}>{current.settlementMonth}</Text><Text style={styles.monthHint}>월 정산</Text></View>
          <Pressable accessibilityLabel="다음 정산 월" disabled={index === 0} onPress={() => setIndex(index - 1)} style={styles.monthButton}><Ionicons name="chevron-forward" size={18} color={index === 0 ? colors.ink400 : colors.ink900}/></Pressable>
        </View>
        <Panel tone="green">
          <View style={styles.statusRow}><Text style={styles.label}>{current.status === 'COMPLETED' ? '정산 완료 금액' : '예상 정산금'}</Text><View style={styles.statusBadge}><View style={styles.statusDot}/><Text style={styles.statusText}>{current.status === 'COMPLETED' ? '지급 완료' : current.status === 'FAILED' ? '정산 실패' : '처리 중'}</Text></View></View>
          <Text style={styles.total}>{Number(current.settlementAmount).toLocaleString()}원</Text>
        </Panel>
        <Panel>
          <Text style={styles.heading}>정산 상세</Text>
          <Row label="판매 금액" value={`${Number(current.grossAmount).toLocaleString()}원`} />
          <Row label="플랫폼 수수료" value={`−${Number(current.feeAmount).toLocaleString()}원`} />
          <Row label="정산 대상 주문" value={`${current.orderCount}건`} />
        </Panel>
      </>}
    </SellerShell>
  );
}

const styles = StyleSheet.create({
  month: { minHeight: 58, paddingHorizontal: 6, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', borderRadius: radius.input, backgroundColor: colors.white, borderWidth: 1, borderColor: colors.line },
  monthButton: { width: 44, height: 44, alignItems: 'center', justifyContent: 'center' },
  monthCopy: { alignItems: 'center' },
  monthText: { color: colors.ink900, fontFamily: fonts.body, fontSize: 17, fontWeight: '800' },
  monthHint: { marginTop: 2, color: colors.ink400, fontFamily: fonts.body, fontSize: 10 },
  statusRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  statusBadge: { minHeight: 28, paddingHorizontal: 9, flexDirection: 'row', alignItems: 'center', gap: 6, borderRadius: radius.pill, backgroundColor: colors.white },
  statusDot: { width: 6, height: 6, borderRadius: 3, backgroundColor: colors.green500 },
  statusText: { color: colors.green700, fontFamily: fonts.body, fontSize: 10, fontWeight: '800' },
  label: { color: colors.green700, fontFamily: fonts.body, fontSize: 12, fontWeight: '700' },
  total: { marginTop: 10, color: colors.ink900, fontFamily: fonts.body, fontSize: 31, fontWeight: '800', letterSpacing: -1.1 },
  heading: { marginBottom: 7, color: colors.ink900, fontFamily: fonts.body, fontSize: 17, fontWeight: '800' },
});
