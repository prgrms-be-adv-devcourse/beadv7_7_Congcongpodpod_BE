import { RoundedIcon as Ionicons } from '@/components/rounded-icon';
import { router } from 'expo-router';
import type { PropsWithChildren } from 'react';
import { KeyboardAvoidingView, Platform, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { ScreenEntrance } from '@/components/motion';
import { AppRefreshControl } from '@/components/app-refresh-control';
import { FLOATING_TAB_CONTENT_INSET } from '@/components/floating-tab-bar';
import { RefreshStatus } from '@/components/refresh-status';
import { colors, fonts, layout } from '@/constants/theme';
import { useResponsiveLayout } from '@/hooks/use-responsive-layout';

export function SellerShell({ title, description, children, back, storeName = '내 매장', storeStatus = 'OPEN', refreshing = false, onRefresh }: PropsWithChildren<{ title: string; description: string; back?: boolean; storeName?: string; storeStatus?: string; refreshing?: boolean; onRefresh?: () => void }>) {
  const { width, contentWidth, gutter, isCompact, isTablet } = useResponsiveLayout();
  const shellWidth = isTablet ? Math.min(width, layout.wide) : contentWidth;
  const statusLabel = storeName === '미등록' ? '등록 필요' : storeStatus === 'OPEN' ? '운영 중' : storeStatus === 'STOPPED' ? '운영 중지' : '오픈 전';
  const isOpen = storeStatus === 'OPEN' && storeName !== '미등록';
  return <SafeAreaView style={styles.safe} edges={['top']}>
    <View style={styles.topBar}>
      <View style={[styles.store, { width: shellWidth, paddingHorizontal: gutter }]}>
        {back ? <Pressable accessibilityLabel="뒤로 가기" style={styles.back} onPress={() => router.back()}><Ionicons name="chevron-back" size={23} color={colors.ink900}/></Pressable> : <View style={styles.storeIcon}><Ionicons name="storefront-outline" size={18} color={colors.white}/></View>}
        <View style={styles.storeCopy}><Text style={styles.label}>운영 매장</Text><Text numberOfLines={1} style={styles.storeName}>{storeName}</Text></View>
        <View style={[styles.openState, !isOpen && styles.closedState]}><View style={[styles.openDot, !isOpen && styles.closedDot]}/><Text style={[styles.openText, !isOpen && styles.closedText]}>{statusLabel}</Text></View>
      </View>
    </View>
    <View style={[styles.heading, { width: shellWidth, paddingHorizontal: gutter }]}><Text style={[styles.title, isCompact && styles.titleCompact]}>{title}</Text><Text style={styles.description}>{description}</Text></View><RefreshStatus visible={refreshing}/>
    <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} style={styles.keyboard}>
      <ScreenEntrance><ScrollView alwaysBounceVertical={Boolean(onRefresh)} automaticallyAdjustKeyboardInsets={Platform.OS === 'ios'} contentContainerStyle={[styles.content, { width: shellWidth, paddingHorizontal: gutter, paddingBottom: FLOATING_TAB_CONTENT_INSET }]} keyboardDismissMode={Platform.OS === 'ios' ? 'interactive' : 'on-drag'} keyboardShouldPersistTaps="handled" refreshControl={onRefresh ? <AppRefreshControl refreshing={refreshing} onRefresh={onRefresh}/> : undefined} showsVerticalScrollIndicator={false}>
        {children}
      </ScrollView></ScreenEntrance>
    </KeyboardAvoidingView>
  </SafeAreaView>;
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.canvasWarm },
  keyboard: { flex: 1 },
  topBar: { backgroundColor: colors.white, borderBottomWidth: 1, borderBottomColor: colors.line },
  store: { minHeight: 62, alignSelf: 'center', flexDirection: 'row', alignItems: 'center' },
  back: { width: 44, height: 44, marginLeft: -10, marginRight: 2, alignItems: 'center', justifyContent: 'center' },
  storeIcon: { width: 38, height: 38, marginRight: 11, alignItems: 'center', justifyContent: 'center', borderRadius: 11, backgroundColor: colors.ink900 },
  storeCopy: { flex: 1, minWidth: 0 },
  label: { color: colors.ink400, fontFamily: fonts.body, fontSize: 9, fontWeight: '700' },
  storeName: { marginTop: 2, color: colors.ink900, fontFamily: fonts.body, fontSize: 15, fontWeight: '900' },
  openState: { minHeight: 28, paddingHorizontal: 9, flexDirection: 'row', alignItems: 'center', gap: 5, borderWidth: 1, borderColor: colors.lineStrong, borderRadius: 14, backgroundColor: colors.white },
  openDot: { width: 6, height: 6, borderRadius: 3, backgroundColor: colors.green500 },
  openText: { color: colors.green900, fontFamily: fonts.body, fontSize: 9, fontWeight: '800' },
  closedState: { borderColor: colors.lineStrong, backgroundColor: colors.white },
  closedDot: { backgroundColor: colors.ink400 },
  closedText: { color: colors.ink700 },
  content: { alignSelf: 'center', paddingTop: 8, gap: 16 },
  heading: { alignSelf: 'center', paddingTop: 16, paddingBottom: 7 },
  title: { color: colors.ink900, fontFamily: fonts.body, fontSize: 28, lineHeight: 35, fontWeight: '900', letterSpacing: -1.1 },
  titleCompact: { fontSize: 25, lineHeight: 32 },
  description: { maxWidth: 560, marginTop: 6, color: colors.ink700, fontFamily: fonts.body, fontSize: 13, lineHeight: 20 },
});
