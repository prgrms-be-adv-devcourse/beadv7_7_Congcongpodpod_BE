import { Ionicons } from '@expo/vector-icons';
import { router, useFocusEffect } from 'expo-router';
import { useCallback, useState } from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { BrandLogo } from '@/components/brand-logo';
import { AppRefreshControl } from '@/components/app-refresh-control';
import { ConfirmModal } from '@/components/confirm-modal';
import { LoadingState } from '@/components/loading-state';
import { RefreshStatus } from '@/components/refresh-status';
import { colors, fonts, radius, shadow } from '@/constants/theme';
import { useResponsiveLayout } from '@/hooks/use-responsive-layout';
import { usePullToRefresh } from '@/hooks/use-pull-to-refresh';
import { getDepositBalance } from '@/lib/account';
import { getMyOrderCount } from '@/lib/orders';
import { temporaryMemberStats as memberStats } from '@/lib/member-stats';
import { useAuth } from '@/providers/auth-provider';

type MenuRowProps = {
  icon: keyof typeof Ionicons.glyphMap;
  label: string;
  description?: string;
  onPress?: () => void;
  danger?: boolean;
};

function MenuRow({ icon, label, description, onPress, danger }: MenuRowProps) {
  return (
    <Pressable accessibilityRole="button" onPress={onPress} style={({ pressed }) => [styles.menuRow, pressed && styles.pressed]}>
      <View style={[styles.menuIcon, danger && styles.menuIconDanger]}><Ionicons name={icon} size={18} color={danger ? colors.danger700 : colors.ink700}/></View>
      <View style={styles.menuCopy}><Text style={[styles.menuText, danger && styles.menuTextDanger]}>{label}</Text>{description ? <Text numberOfLines={1} style={styles.menuDescription}>{description}</Text> : null}</View>
      <Ionicons name="chevron-forward" size={17} color={colors.ink400}/>
    </Pressable>
  );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return <View style={styles.section}><Text style={styles.sectionTitle}>{title}</Text><View style={styles.sectionSurface}>{children}</View></View>;
}

export default function MyScreen() {
  const { member, initializing, signOut, refreshProfile } = useAuth();
  const { contentWidth, gutter, isCompact } = useResponsiveLayout();
  const [depositBalance, setDepositBalance] = useState<number | null>(null);
  const [orderCount, setOrderCount] = useState<number | null>(null);

  const openSeller = () => router.push(member?.role === 'SELLER' ? '/seller/home' : '/seller/store');

  const load = useCallback(async () => {
    if (!member) {
      setDepositBalance(null);
      setOrderCount(null);
      return;
    }
    try {
      const [balance, count] = await Promise.all([getDepositBalance(), getMyOrderCount()]);
      setDepositBalance(balance);
      setOrderCount(count);
    } catch {
      setDepositBalance(null);
      setOrderCount(null);
    }
  }, [member]);
  useFocusEffect(useCallback(() => { void load(); }, [load]));
  const refreshAll = useCallback(async () => {
    if (member) await refreshProfile();
    await load();
  }, [load, member, refreshProfile]);
  const { refreshing, onRefresh } = usePullToRefresh(refreshAll);

  if (initializing) return <SafeAreaView style={styles.center}><LoadingState label="내 정보를 불러오고 있어요"/></SafeAreaView>;
  if (!member) return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      <View style={[styles.fixedHeader, { width: contentWidth, paddingHorizontal: gutter }]}><Text style={[styles.title, isCompact && styles.titleCompact]}>마이페이지</Text><Text style={styles.headerDescription}>내 주문과 혜택을 한곳에서 관리하세요.</Text></View>
      <ScrollView contentContainerStyle={[styles.guestScroll, { width: contentWidth, paddingHorizontal: gutter }]} showsVerticalScrollIndicator={false}>
        <GuestContent/>
      </ScrollView>
    </SafeAreaView>
  );

  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      <View style={[styles.fixedHeader, { width: contentWidth, paddingHorizontal: gutter }]}><Text style={[styles.title, isCompact && styles.titleCompact]}>마이페이지</Text>{!member ? <Text style={styles.headerDescription}>내 주문과 혜택을 한곳에서 관리하세요.</Text> : null}</View><RefreshStatus visible={refreshing}/>
      <View style={styles.body}><ScrollView alwaysBounceVertical style={styles.scrollView} contentContainerStyle={styles.scroll} refreshControl={<AppRefreshControl refreshing={refreshing} onRefresh={onRefresh}/>} showsVerticalScrollIndicator={false}>
        <View style={[styles.content, { width: contentWidth, paddingHorizontal: gutter }]}> 
          <SignedInContent memberName={member.name} role={member.role} depositBalance={depositBalance} orderCount={orderCount} openSeller={openSeller} signOut={signOut}/>
        </View>
      </ScrollView></View>
    </SafeAreaView>
  );
}

function SignedInContent({ memberName, role, depositBalance, orderCount, openSeller, signOut }: { memberName: string; role: string; depositBalance: number | null; orderCount: number | null; openSeller: () => void; signOut: () => Promise<void> }) {
  const [signOutConfirming, setSignOutConfirming] = useState(false);
  const [signingOut, setSigningOut] = useState(false);
  const confirmSignOut = async () => {
    try {
      setSigningOut(true);
      await signOut();
      setSignOutConfirming(false);
    } finally {
      setSigningOut(false);
    }
  };

  return <>
    <View style={styles.profileCard}>
      <Pressable accessibilityRole="button" onPress={() => router.push('/profile')} style={styles.profilePress}><View style={styles.profileTop}><View style={styles.avatar}><BrandLogo size={54}/></View><View style={styles.profileCopy}><View style={styles.nameRow}><Text style={styles.memberName}>{memberName}님</Text><View style={styles.roleBadge}><Text style={styles.roleText}>{role === 'SELLER' ? '사장님' : '고객'}</Text></View></View><Text style={styles.profileLink}>내 정보와 활동 자세히 보기</Text></View><Ionicons name="chevron-forward" size={18} color={colors.ink400}/></View><View style={styles.memberStats}><ProfileStat label="등급" value={memberStats.grade}/><ProfileStat label="포인트" value={`${memberStats.points.toLocaleString()}P`}/></View></Pressable>
    </View>

    <View style={styles.wallet}>
      <Pressable accessibilityRole="button" onPress={() => router.push('/deposits')} style={styles.walletCopy}><View style={styles.walletLabelRow}><Ionicons name="wallet-outline" size={16} color={colors.green300}/><Text style={styles.walletLabel}>사용 가능 예치금</Text></View><Text style={styles.walletValue}>{depositBalance === null ? '—' : `${depositBalance.toLocaleString()}원`}</Text><Text style={styles.walletHint}>예치금 내역 보기  ›</Text></Pressable>
      <Pressable accessibilityRole="button" onPress={() => router.push('/deposits/charge')} style={({ pressed }) => [styles.chargeButton, pressed && styles.pressed]}><Ionicons name="add" size={17} color={colors.ink900}/><Text style={styles.chargeText}>충전</Text></Pressable>
    </View>

    <View style={styles.quickRow}>
      <QuickItem icon="receipt-outline" value={orderCount === null ? '—' : String(orderCount)} label="주문" onPress={() => router.push('/orders')}/>
      <QuickItem icon="heart-outline" value="보기" label="찜" onPress={() => router.push('/favorites')}/>
      <QuickItem icon="star-outline" value="관리" label="리뷰"/>
    </View>

    <Section title="나의 활동">
      <MenuRow icon="wallet-outline" label="예치금 이용내역" description="충전·결제·환불 기록" onPress={() => router.push('/deposits')}/>
      <MenuRow icon="chatbox-ellipses-outline" label="리뷰 관리" description="작성한 리뷰와 답글"/>
      <MenuRow icon="notifications-outline" label="알림 설정" description="주문과 마감 할인 알림"/>
    </Section>

    <Section title="사장님 메뉴">
      <MenuRow icon="storefront-outline" label={role === 'SELLER' ? '내 매장 대시보드' : '상점 등록하고 판매 시작'} description={role === 'SELLER' ? '상품·주문·정산 관리' : '상점 정보를 등록하면 권한이 변경돼요'} onPress={openSeller}/>
    </Section>

    <Section title="고객지원">
      <MenuRow icon="help-circle-outline" label="공지사항 · 문의하기"/>
      <MenuRow icon="log-out-outline" label="로그아웃" danger onPress={() => setSignOutConfirming(true)}/>
    </Section>
    <ConfirmModal visible={signOutConfirming} icon="log-out-outline" title="로그아웃할까요?" description="현재 기기에 저장된 로그인 정보가 삭제됩니다." confirmLabel="로그아웃" busy={signingOut} busyLabel="로그아웃 중…" tone="danger" onCancel={() => setSignOutConfirming(false)} onConfirm={() => void confirmSignOut()}/>
  </>;
}

function GuestContent() {
  return <>
    <View style={styles.guestHero}><BrandLogo size={76}/><Text style={styles.guestTitle}>로그인하고 더 편하게 픽업하세요</Text><Text style={styles.guestBody}>주문 상태, 예치금, 찜한 매장을 한곳에서 확인할 수 있어요.</Text><Pressable accessibilityRole="button" onPress={() => router.push('/login')} style={({ pressed }) => [styles.loginButton, pressed && styles.pressed]}><Text style={styles.loginButtonText}>로그인</Text></Pressable><Pressable accessibilityRole="button" onPress={() => router.push('/signup')} style={styles.signupButton}><Text style={styles.signupText}>처음이신가요? <Text style={styles.signupAccent}>회원가입</Text></Text></Pressable></View>
    <Section title="도움말"><MenuRow icon="megaphone-outline" label="공지사항"/><MenuRow icon="headset-outline" label="고객센터"/><MenuRow icon="document-text-outline" label="서비스 이용약관"/></Section>
  </>;
}

function QuickItem({ icon, value, label, onPress }: { icon: keyof typeof Ionicons.glyphMap; value: string; label: string; onPress?: () => void }) {
  return <Pressable accessibilityRole="button" onPress={onPress} style={({ pressed }) => [styles.quickItem, pressed && styles.pressed]}><Ionicons name={icon} size={19} color={colors.ink700}/><Text style={styles.quickValue}>{value}</Text><Text style={styles.quickLabel}>{label}</Text></Pressable>;
}
function ProfileStat({ label, value }: { label: string; value: string }) { return <View style={styles.profileStat}><Text style={styles.profileStatLabel}>{label}</Text><Text numberOfLines={1} style={styles.profileStatValue}>{value}</Text></View>; }

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.white },
  body: { flex: 1 },
  scrollView: { flex: 1 },
  guestScroll: { alignSelf: 'center', flexGrow: 1, paddingBottom: 28 },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.white },
  scroll: { paddingBottom: 28 }, fixedHeader: { alignSelf: 'center', paddingTop: 20, paddingBottom: 17, backgroundColor: colors.white },
  content: { alignSelf: 'center' },
  header: { paddingTop: 20, paddingBottom: 17 },
  title: { color: colors.ink900, fontFamily: fonts.body, fontSize: 28, lineHeight: 35, fontWeight: '900', letterSpacing: -1.1 },
  titleCompact: { fontSize: 25, lineHeight: 32 },
  headerDescription: { marginTop: 5, color: colors.ink700, fontFamily: fonts.body, fontSize: 13 },
  profileCard: { borderRadius: 12, backgroundColor: colors.white, borderWidth: 1, borderColor: colors.line, ...shadow.card },
  profilePress: { padding: 13, paddingBottom: 9 },
  profileTop: { minHeight: 58, flexDirection: 'row', alignItems: 'center', gap: 12 },
  avatar: { width: 52, height: 52, alignItems: 'center', justifyContent: 'center', overflow: 'hidden', borderRadius: 26, backgroundColor: colors.canvas },
  profileCopy: { flex: 1, minHeight: 58, justifyContent: 'center' },
  nameRow: { flexDirection: 'row', alignItems: 'center', gap: 7 },
  memberName: { color: colors.ink900, fontFamily: fonts.body, fontSize: 19, fontWeight: '900', letterSpacing: -0.5 },
  roleBadge: { paddingHorizontal: 7, paddingVertical: 3, borderRadius: radius.pill, backgroundColor: colors.canvas },
  roleText: { color: colors.ink700, fontFamily: fonts.body, fontSize: 9, fontWeight: '800' },
  profileLink: { marginTop: 5, color: colors.ink500, fontFamily: fonts.body, fontSize: 11 },
  memberStats: { marginTop: 8, paddingTop: 8, flexDirection: 'row', borderTopWidth: StyleSheet.hairlineWidth, borderTopColor: colors.line },
  profileStat: { width: '50%' },
  profileStatLabel: { color: colors.ink500, fontFamily: fonts.body, fontSize: 10, fontWeight: '700' },
  profileStatValue: { marginTop: 2, color: colors.ink900, fontFamily: fonts.body, fontSize: 14, lineHeight: 18, fontWeight: '900', letterSpacing: -0.25 },
  wallet: { minHeight: 126, marginTop: 12, padding: 17, flexDirection: 'row', alignItems: 'center', gap: 12, borderRadius: 13, backgroundColor: colors.ink900, ...shadow.card },
  walletCopy: { flex: 1 },
  walletLabelRow: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  walletLabel: { color: colors.ink400, fontFamily: fonts.body, fontSize: 11, fontWeight: '700' },
  walletValue: { marginTop: 7, color: colors.white, fontFamily: fonts.body, fontSize: 27, fontWeight: '900', letterSpacing: -1 },
  walletHint: { marginTop: 6, color: colors.ink400, fontFamily: fonts.body, fontSize: 10 },
  chargeButton: { minWidth: 70, minHeight: 44, paddingHorizontal: 13, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 3, borderRadius: 9, backgroundColor: colors.white },
  chargeText: { color: colors.ink900, fontFamily: fonts.body, fontSize: 12, fontWeight: '900' },
  quickRow: { minHeight: 88, marginTop: 12, flexDirection: 'row', overflow: 'hidden', borderRadius: 12, backgroundColor: colors.canvas, borderWidth: 1, borderColor: colors.line },
  quickItem: { flex: 1, minHeight: 80, alignItems: 'center', justifyContent: 'center' },
  quickValue: { marginTop: 5, color: colors.ink900, fontFamily: fonts.body, fontSize: 14, fontWeight: '900' },
  quickLabel: { marginTop: 2, color: colors.ink500, fontFamily: fonts.body, fontSize: 10, fontWeight: '600' },
  section: { marginTop: 24 },
  sectionTitle: { marginBottom: 9, color: colors.ink900, fontFamily: fonts.body, fontSize: 16, fontWeight: '900' },
  sectionSurface: { paddingHorizontal: 13, overflow: 'hidden', borderRadius: 12, backgroundColor: colors.white, borderWidth: 1, borderColor: colors.line },
  menuRow: { minHeight: 66, flexDirection: 'row', alignItems: 'center', gap: 11, borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: colors.line },
  menuIcon: { width: 36, height: 36, alignItems: 'center', justifyContent: 'center', borderRadius: 10, backgroundColor: colors.canvas },
  menuIconDanger: { backgroundColor: colors.danger50 },
  menuCopy: { flex: 1 },
  menuText: { color: colors.ink900, fontFamily: fonts.body, fontSize: 13, fontWeight: '800' },
  menuTextDanger: { color: colors.danger700 },
  menuDescription: { marginTop: 3, color: colors.ink500, fontFamily: fonts.body, fontSize: 10 },
  pressed: { opacity: 0.65, transform: [{ scale: 0.99 }] },
  guestHero: { padding: 23, alignItems: 'center', borderRadius: 13, backgroundColor: colors.canvas, borderWidth: 1, borderColor: colors.line },
  guestTitle: { marginTop: 8, color: colors.ink900, fontFamily: fonts.body, fontSize: 20, fontWeight: '900', letterSpacing: -0.6, textAlign: 'center' },
  guestBody: { maxWidth: 320, marginTop: 7, color: colors.ink700, fontFamily: fonts.body, fontSize: 13, lineHeight: 20, textAlign: 'center' },
  loginButton: { width: '100%', maxWidth: 360, minHeight: 48, marginTop: 18, alignItems: 'center', justifyContent: 'center', borderRadius: 10, backgroundColor: colors.ink900 },
  loginButtonText: { color: colors.white, fontFamily: fonts.body, fontSize: 14, fontWeight: '900' },
  signupButton: { minHeight: 44, justifyContent: 'center' },
  signupText: { color: colors.ink700, fontFamily: fonts.body, fontSize: 12 },
  signupAccent: { color: colors.green700, fontWeight: '900' },
});
