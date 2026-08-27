import { RoundedIcon as Ionicons } from '@/components/rounded-icon';
import { router, useFocusEffect } from 'expo-router';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';

import { Page } from '@/components/page';
import { Pagination } from '@/components/pagination';
import { colors, fonts, radius, shadow } from '@/constants/theme';
import { useMemberBenefits } from '@/hooks/use-member-benefits';
import { usePullToRefresh } from '@/hooks/use-pull-to-refresh';
import { subscribeMemberBenefitsChanged } from '@/lib/member-benefit-events';
import { getPointHistory, type PointHistory } from '@/lib/member-stats';

const PAGE_SIZE = 7;
const policies = [
  { icon: 'bag-check-outline', title: '픽업 완료 후 적립', description: '정상적으로 픽업이 완료된 주문만 포인트가 적립돼요.' },
  { icon: 'close-circle-outline', title: '취소·미수령은 제외', description: '취소되거나 미수령 처리된 주문은 적립 대상이 아니에요.' },
  { icon: 'information-circle-outline', title: '세부 기준은 준비 중', description: '적립률과 사용 기준은 정책 확정 후 안내할 예정이에요.' },
] as const;
const historyLabels: Record<PointHistory['type'], string> = { EARN: '포인트 적립', USE: '포인트 사용', EXPIRE: '포인트 소멸', REFUND: '포인트 환급' };

export default function PointsScreen() {
  const [policyPage, setPolicyPage] = useState(0);
  const [historyPage, setHistoryPage] = useState(0);
  const [pointHistory, setPointHistory] = useState<PointHistory[]>([]);
  const { points, refresh } = useMemberBenefits();
  const loadHistory = useCallback(async () => { try { setPointHistory(await getPointHistory(0, 50)); } catch {} }, []);
  useFocusEffect(useCallback(() => { void loadHistory(); }, [loadHistory]));
  useEffect(() => subscribeMemberBenefitsChanged(() => { void loadHistory(); }), [loadHistory]);
  const refreshAll = useCallback(async () => { await Promise.all([refresh(), loadHistory()]); }, [loadHistory, refresh]);
  const { refreshing, onRefresh } = usePullToRefresh(refreshAll);
  const policyPages = Math.ceil(policies.length / PAGE_SIZE);
  const historyPages = Math.ceil(pointHistory.length / PAGE_SIZE);
  const visiblePolicies = useMemo(() => policies.slice(policyPage * PAGE_SIZE, (policyPage + 1) * PAGE_SIZE), [policyPage]);
  const visibleHistory = useMemo(() => pointHistory.slice(historyPage * PAGE_SIZE, (historyPage + 1) * PAGE_SIZE), [historyPage, pointHistory]);
  return <Page title="라디 포인트" description="포인트 잔액과 적립·사용내역을 확인하세요." refreshing={refreshing} onRefresh={onRefresh} onClose={() => router.replace('/my')} closeLabel="포인트 상세 닫기">
    <View style={styles.balanceCard}>
      <View style={styles.balanceTop}>
        <View style={styles.balanceBrand}><View style={styles.balanceIcon}><Ionicons name="sparkles" size={18} color={colors.green300}/></View><Text style={styles.pointBrand}>라디 포인트</Text></View>
        <Text style={styles.earnLabel}>픽업으로 쌓는 혜택</Text>
      </View>
      <Text style={styles.balanceLabel}>사용 가능한 포인트</Text>
      <Text style={styles.balanceValue}>{points === null ? '—' : `${points.toLocaleString()}P`}</Text>
      <View style={styles.balanceBottom}>
        <Text style={styles.balanceHint}>픽업을 완료할수록 포인트와 등급 혜택이 쌓여요.</Text>
        <View style={styles.statusBadge}><View style={styles.statusDot}/><Text style={styles.statusText}>적립 준비 중</Text></View>
      </View>
    </View>
    <View style={styles.historyHead}><Text style={styles.heading}>사용내역</Text><Text style={styles.historyMeta}>{pointHistory.length}건</Text></View>
    {visibleHistory.length ? <View style={styles.history}>{visibleHistory.map(item => <View key={item.historyId} style={styles.historyRow}><View style={styles.historyCopy}><Text style={styles.historyTitle}>{historyLabels[item.type]}</Text><Text style={styles.historyDate}>{new Date(item.createdAt).toLocaleDateString('ko-KR')}</Text></View><View><Text style={[styles.historyAmount, item.amount > 0 && styles.historyEarn]}>{item.amount > 0 ? '+' : ''}{item.amount.toLocaleString()}P</Text><Text style={styles.historyBalance}>{item.balanceAfter.toLocaleString()}P</Text></View></View>)}</View> : <View style={styles.empty}><Ionicons name="receipt-outline" size={24} color={colors.ink400}/><Text style={styles.emptyTitle}>아직 포인트 내역이 없어요</Text><Text style={styles.emptyText}>적립하거나 사용하면 이곳에 기록됩니다.</Text></View>}
    <Pagination page={historyPage} totalPages={historyPages} onChange={setHistoryPage}/>
    <Text style={styles.heading}>포인트 정책</Text>
    <View style={styles.policy}>{visiblePolicies.map((policy, index) => <Policy key={policy.title} {...policy} last={index === visiblePolicies.length - 1}/>)}</View>
    <Pagination page={policyPage} totalPages={policyPages} onChange={setPolicyPage}/>
  </Page>;
}

function Policy({ icon, title, description, last }: { icon: keyof typeof Ionicons.glyphMap; title: string; description: string; last?: boolean }) { return <View style={[styles.policyRow, last && styles.last]}><View style={styles.policyIcon}><Ionicons name={icon} size={17} color={colors.green700}/></View><View style={styles.policyCopy}><Text style={styles.policyTitle}>{title}</Text><Text style={styles.policyDescription}>{description}</Text></View></View>; }
const styles = StyleSheet.create({ balanceCard: { padding: 18, borderRadius: radius.card, backgroundColor: colors.ink900, borderWidth: 1, borderColor: '#2B352F', ...shadow.card }, balanceTop: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' }, balanceBrand: { flexDirection: 'row', alignItems: 'center', gap: 7 }, balanceIcon: { width: 32, height: 32, alignItems: 'center', justifyContent: 'center', borderRadius: 10, backgroundColor: 'rgba(3,199,90,0.14)' }, pointBrand: { color: colors.white, fontFamily: fonts.body, fontSize: 12, fontWeight: '900' }, earnLabel: { color: colors.green200, fontFamily: fonts.body, fontSize: 9, fontWeight: '700' }, balanceLabel: { marginTop: 19, color: colors.ink400, fontFamily: fonts.body, fontSize: 11, fontWeight: '700' }, balanceValue: { marginTop: 4, color: colors.white, fontFamily: fonts.body, fontSize: 31, fontWeight: '900', letterSpacing: -1.2 }, balanceBottom: { marginTop: 18, flexDirection: 'row', alignItems: 'center', gap: 12 }, balanceHint: { flex: 1, color: colors.ink400, fontFamily: fonts.body, fontSize: 10, lineHeight: 15 }, statusBadge: { minHeight: 32, paddingHorizontal: 10, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 5, borderRadius: radius.pill, backgroundColor: 'rgba(3,199,90,0.13)' }, statusDot: { width: 5, height: 5, borderRadius: radius.pill, backgroundColor: colors.green300 }, statusText: { color: colors.green200, fontFamily: fonts.body, fontSize: 9, fontWeight: '800' }, heading: { marginTop: 8, color: colors.ink900, fontFamily: fonts.body, fontSize: 17, fontWeight: '900' }, policy: { paddingHorizontal: 14, overflow: 'hidden', borderRadius: radius.card, backgroundColor: colors.white, borderWidth: 1, borderColor: colors.line }, policyRow: { minHeight: 74, flexDirection: 'row', alignItems: 'center', gap: 11, borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: colors.line }, last: { borderBottomWidth: 0 }, policyIcon: { width: 34, height: 34, alignItems: 'center', justifyContent: 'center', borderRadius: 10, backgroundColor: colors.green50 }, policyCopy: { flex: 1 }, policyTitle: { color: colors.ink900, fontFamily: fonts.body, fontSize: 13, fontWeight: '800' }, policyDescription: { marginTop: 4, color: colors.ink500, fontFamily: fonts.body, fontSize: 10, lineHeight: 15 }, historyHead: { minHeight: 42, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' }, historyMeta: { color: colors.ink500, fontFamily: fonts.body, fontSize: 11, fontWeight: '700' }, history: { overflow: 'hidden', borderRadius: radius.card, backgroundColor: colors.white, borderWidth: 1, borderColor: colors.line }, historyRow: { minHeight: 66, paddingHorizontal: 15, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: colors.line }, historyCopy: { flex: 1 }, historyTitle: { color: colors.ink900, fontFamily: fonts.body, fontSize: 13, fontWeight: '800' }, historyDate: { marginTop: 4, color: colors.ink500, fontFamily: fonts.body, fontSize: 9 }, historyAmount: { color: colors.ink900, fontFamily: fonts.body, fontSize: 13, fontWeight: '900', textAlign: 'right' }, historyEarn: { color: colors.green700 }, historyBalance: { marginTop: 4, color: colors.ink500, fontFamily: fonts.body, fontSize: 9, textAlign: 'right' }, empty: { minHeight: 150, alignItems: 'center', justifyContent: 'center', borderRadius: radius.card, backgroundColor: colors.white, borderWidth: 1, borderColor: colors.line }, emptyTitle: { marginTop: 10, color: colors.ink900, fontFamily: fonts.body, fontSize: 14, fontWeight: '800' }, emptyText: { marginTop: 5, color: colors.ink500, fontFamily: fonts.body, fontSize: 10 }, });
