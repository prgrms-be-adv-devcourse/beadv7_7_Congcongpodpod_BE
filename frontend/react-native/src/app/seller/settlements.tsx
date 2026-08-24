import { RoundedIcon as Ionicons } from '@/components/rounded-icon';
import { useCallback, useEffect, useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { EmptyState } from '@/components/empty-state';
import { LoadingState } from '@/components/loading-state';
import { SellerShell } from '@/components/seller-shell';
import { colors, fonts, radius } from '@/constants/theme';
import { usePullToRefresh } from '@/hooks/use-pull-to-refresh';
import { useResponsiveLayout } from '@/hooks/use-responsive-layout';
import { getMyStores, getSettlements, type Settlement } from '@/lib/seller';

type StatusVisual = { label: string; color: string; dot: string };

function getStatusVisual(status: string): StatusVisual {
  if (status === 'COMPLETED') return { label: '지급 완료', color: colors.green700, dot: colors.green500 };
  if (status === 'FAILED') return { label: '정산 실패', color: colors.danger700, dot: colors.danger700 };
  return { label: '처리 중', color: colors.ink700, dot: colors.ink400 };
}

export default function Settlements() {
  const { isTablet } = useResponsiveLayout();
  const [items, setItems] = useState<Settlement[]>([]);
  const [index, setIndex] = useState(0);
  const [storeName, setStoreName] = useState('내 매장');
  const [loading, setLoading] = useState(true);
  const [failed, setFailed] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [stores, settlements] = await Promise.all([getMyStores(), getSettlements()]);
      const store = stores[0];
      setStoreName(store?.storeName ?? '미등록');
      setItems(store ? settlements.filter((item) => item.storeId === store.storeId) : []);
      setIndex(0);
      setFailed(false);
    } catch {
      setFailed(true);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { void load(); }, [load]);
  const { refreshing, onRefresh } = usePullToRefresh(load);
  const current = items[index];
  const status = current ? getStatusVisual(current.status) : undefined;

  return (
    <SellerShell title="정산" description="월별 매출과 지급 상태를 확인하세요." storeName={storeName} refreshing={refreshing} onRefresh={onRefresh}>
      {loading && !items.length ? (
        <LoadingState label="정산 내역을 불러오고 있어요" compact />
      ) : failed && !current ? (
        <EmptyState title="정산 내역을 불러오지 못했어요" description="연결 상태를 확인한 뒤 다시 시도해주세요." actionLabel="다시 불러오기" onAction={() => void load()} />
      ) : !current ? (
        <EmptyState title="정산 내역이 없어요" description="판매가 시작되면 월별 정산 금액을 확인할 수 있어요." />
      ) : (
        <>
          <View style={styles.month}>
            <Pressable accessibilityLabel="이전 정산 월" disabled={index >= items.length - 1} onPress={() => setIndex((value) => value + 1)} style={styles.monthButton}>
              <Ionicons name="chevron-back" size={18} color={index >= items.length - 1 ? colors.ink400 : colors.ink900} />
            </Pressable>
            <View style={styles.monthCopy}>
              <Text style={styles.monthText}>{current.settlementMonth}</Text>
              <Text style={styles.monthHint}>월 정산</Text>
            </View>
            <Pressable accessibilityLabel="다음 정산 월" disabled={index === 0} onPress={() => setIndex((value) => value - 1)} style={styles.monthButton}>
              <Ionicons name="chevron-forward" size={18} color={index === 0 ? colors.ink400 : colors.ink900} />
            </Pressable>
          </View>

          {failed ? (
            <View style={styles.staleNotice}>
              <Text style={styles.staleText}>최신 정산 정보를 확인하지 못했어요. 마지막 내역을 표시합니다.</Text>
              <Pressable accessibilityRole="button" onPress={() => void load()} style={styles.retryButton}><Text style={styles.retryText}>새로고침</Text></Pressable>
            </View>
          ) : null}

          <View style={[styles.summaryGrid, isTablet && styles.summaryGridWide]}>
            <View style={[styles.hero, isTablet && styles.heroWide]}>
              <View style={styles.statusRow}>
                <Text style={styles.label}>{current.status === 'COMPLETED' ? '정산 완료 금액' : '예상 정산금'}</Text>
                <View style={styles.statusBadge}>
                  <View style={[styles.statusDot, { backgroundColor: status?.dot }]} />
                  <Text style={[styles.statusText, { color: status?.color }]}>{status?.label}</Text>
                </View>
              </View>
              <Text style={styles.total}>{Number(current.settlementAmount).toLocaleString()}원</Text>
              <Text style={styles.heroHint}>수수료를 제외한 최종 지급 예정 금액</Text>
            </View>

            <View style={[styles.detail, isTablet && styles.detailWide]}>
              <Text style={styles.heading}>정산 상세</Text>
              <SettlementRow label="판매 금액" value={`${Number(current.grossAmount).toLocaleString()}원`} />
              <SettlementRow label="플랫폼 수수료" value={`−${Number(current.feeAmount).toLocaleString()}원`} />
              <SettlementRow label="정산 대상 주문" value={`${current.orderCount.toLocaleString()}건`} last />
            </View>
          </View>
        </>
      )}
    </SellerShell>
  );
}

function SettlementRow({ label, value, last = false }: { label: string; value: string; last?: boolean }) {
  return (
    <View style={[styles.detailRow, last && styles.detailRowLast]}>
      <Text style={styles.detailLabel}>{label}</Text>
      <Text style={styles.detailValue}>{value}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  month: { minHeight: 56, paddingHorizontal: 4, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', borderBottomWidth: 1, borderBottomColor: colors.line },
  monthButton: { width: 44, height: 44, alignItems: 'center', justifyContent: 'center' },
  monthCopy: { alignItems: 'center' },
  monthText: { color: colors.ink900, fontFamily: fonts.body, fontSize: 16, fontWeight: '800', fontVariant: ['tabular-nums'] },
  monthHint: { marginTop: 1, color: colors.ink500, fontFamily: fonts.body, fontSize: 10 },
  staleNotice: { minHeight: 44, paddingVertical: 8, paddingHorizontal: 12, flexDirection: 'row', alignItems: 'center', gap: 10, borderRadius: radius.control, backgroundColor: colors.canvas },
  staleText: { flex: 1, color: colors.ink700, fontFamily: fonts.body, fontSize: 11, lineHeight: 16 },
  retryButton: { minHeight: 32, paddingHorizontal: 10, alignItems: 'center', justifyContent: 'center' },
  retryText: { color: colors.green700, fontFamily: fonts.body, fontSize: 11, fontWeight: '800' },
  summaryGrid: { gap: 12 },
  summaryGridWide: { flexDirection: 'row', alignItems: 'stretch' },
  hero: { minHeight: 168, padding: 20, justifyContent: 'space-between', borderWidth: 1, borderColor: colors.lineStrong, borderRadius: radius.card, backgroundColor: colors.white },
  heroWide: { flex: 1.15 },
  statusRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  statusBadge: { minHeight: 28, paddingHorizontal: 10, flexDirection: 'row', alignItems: 'center', gap: 6, borderRadius: radius.pill, backgroundColor: colors.canvas },
  statusDot: { width: 6, height: 6, borderRadius: 3 },
  statusText: { fontFamily: fonts.body, fontSize: 11, fontWeight: '800' },
  label: { color: colors.ink700, fontFamily: fonts.body, fontSize: 12, fontWeight: '700' },
  total: { marginTop: 16, color: colors.ink900, fontFamily: fonts.body, fontSize: 32, fontWeight: '800', letterSpacing: -1.1, fontVariant: ['tabular-nums'] },
  heroHint: { marginTop: 6, color: colors.ink500, fontFamily: fonts.body, fontSize: 11 },
  detail: { paddingHorizontal: 18, borderWidth: 1, borderColor: colors.line, borderRadius: radius.card, backgroundColor: colors.white },
  detailWide: { flex: 0.85 },
  heading: { paddingTop: 17, paddingBottom: 7, color: colors.ink900, fontFamily: fonts.body, fontSize: 16, fontWeight: '800' },
  detailRow: { minHeight: 45, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', borderBottomWidth: 1, borderBottomColor: colors.line },
  detailRowLast: { borderBottomWidth: 0 },
  detailLabel: { color: colors.ink700, fontFamily: fonts.body, fontSize: 12 },
  detailValue: { color: colors.ink900, fontFamily: fonts.body, fontSize: 13, fontWeight: '800', fontVariant: ['tabular-nums'] },
});
