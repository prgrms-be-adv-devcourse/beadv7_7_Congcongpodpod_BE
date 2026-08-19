import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useMemo, useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';

import { Page } from '@/components/page';
import { Pagination } from '@/components/pagination';
import { colors, fonts, radius } from '@/constants/theme';
import { temporaryMemberStats as stats } from '@/lib/member-stats';

const PAGE_SIZE = 7;
const policies = [
  { icon: 'bag-check-outline', title: '픽업 완료 후 적립', description: '정상적으로 픽업이 완료된 주문만 포인트가 적립돼요.' },
  { icon: 'close-circle-outline', title: '취소·미수령은 제외', description: '취소되거나 미수령 처리된 주문은 적립 대상이 아니에요.' },
  { icon: 'information-circle-outline', title: '세부 기준은 준비 중', description: '적립률과 사용 기준은 정책 확정 후 안내할 예정이에요.' },
] as const;
const pointHistory: { id: number; title: string }[] = [];

export default function PointsScreen() {
  const [policyPage, setPolicyPage] = useState(0);
  const [historyPage, setHistoryPage] = useState(0);
  const policyPages = Math.ceil(policies.length / PAGE_SIZE);
  const historyPages = Math.ceil(pointHistory.length / PAGE_SIZE);
  const visiblePolicies = useMemo(() => policies.slice(policyPage * PAGE_SIZE, (policyPage + 1) * PAGE_SIZE), [policyPage]);
  return <Page title="라디 포인트" description="포인트 잔액과 적립·사용내역을 확인하세요." onClose={() => router.replace('/my')} closeLabel="포인트 상세 닫기">
    <View style={styles.balance}><View style={styles.balanceIcon}><Ionicons name="sparkles" size={20} color={colors.green700}/></View><Text style={styles.balanceLabel}>사용 가능한 포인트</Text><Text style={styles.balanceValue}>{stats.points.toLocaleString()}P</Text><Text style={styles.balanceHint}>포인트 사용 기능은 준비 중이에요.</Text></View>
    <View style={styles.historyHead}><Text style={styles.heading}>사용내역</Text><Text style={styles.historyMeta}>0건</Text></View>
    <View style={styles.empty}><Ionicons name="receipt-outline" size={24} color={colors.ink400}/><Text style={styles.emptyTitle}>아직 포인트 내역이 없어요</Text><Text style={styles.emptyText}>적립하거나 사용하면 이곳에 기록됩니다.</Text></View>
    <Pagination page={historyPage} totalPages={historyPages} onChange={setHistoryPage}/>
    <Text style={styles.heading}>포인트 정책</Text>
    <View style={styles.policy}>{visiblePolicies.map((policy, index) => <Policy key={policy.title} {...policy} last={index === visiblePolicies.length - 1}/>)}</View>
    <Pagination page={policyPage} totalPages={policyPages} onChange={setPolicyPage}/>
  </Page>;
}

function Policy({ icon, title, description, last }: { icon: keyof typeof Ionicons.glyphMap; title: string; description: string; last?: boolean }) { return <View style={[styles.policyRow, last && styles.last]}><View style={styles.policyIcon}><Ionicons name={icon} size={17} color={colors.green700}/></View><View style={styles.policyCopy}><Text style={styles.policyTitle}>{title}</Text><Text style={styles.policyDescription}>{description}</Text></View></View>; }
const styles = StyleSheet.create({ balance: { padding: 18, borderRadius: radius.card, backgroundColor: colors.ink900 }, balanceIcon: { width: 38, height: 38, alignItems: 'center', justifyContent: 'center', borderRadius: 12, backgroundColor: colors.green100 }, balanceLabel: { marginTop: 16, color: colors.ink400, fontFamily: fonts.body, fontSize: 11, fontWeight: '700' }, balanceValue: { marginTop: 4, color: colors.white, fontFamily: fonts.body, fontSize: 30, fontWeight: '900', letterSpacing: -1 }, balanceHint: { marginTop: 8, color: colors.ink400, fontFamily: fonts.body, fontSize: 10 }, heading: { marginTop: 8, color: colors.ink900, fontFamily: fonts.body, fontSize: 17, fontWeight: '900' }, policy: { paddingHorizontal: 14, overflow: 'hidden', borderRadius: radius.card, backgroundColor: colors.white, borderWidth: 1, borderColor: colors.line }, policyRow: { minHeight: 74, flexDirection: 'row', alignItems: 'center', gap: 11, borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: colors.line }, last: { borderBottomWidth: 0 }, policyIcon: { width: 34, height: 34, alignItems: 'center', justifyContent: 'center', borderRadius: 10, backgroundColor: colors.green50 }, policyCopy: { flex: 1 }, policyTitle: { color: colors.ink900, fontFamily: fonts.body, fontSize: 13, fontWeight: '800' }, policyDescription: { marginTop: 4, color: colors.ink500, fontFamily: fonts.body, fontSize: 10, lineHeight: 15 }, historyHead: { minHeight: 42, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' }, historyMeta: { color: colors.ink500, fontFamily: fonts.body, fontSize: 11, fontWeight: '700' }, empty: { minHeight: 150, alignItems: 'center', justifyContent: 'center', borderRadius: radius.card, backgroundColor: colors.white, borderWidth: 1, borderColor: colors.line }, emptyTitle: { marginTop: 10, color: colors.ink900, fontFamily: fonts.body, fontSize: 14, fontWeight: '800' }, emptyText: { marginTop: 5, color: colors.ink500, fontFamily: fonts.body, fontSize: 10 }, });
