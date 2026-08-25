import { RoundedIcon as Ionicons } from '@/components/rounded-icon';
import { router } from 'expo-router';
import type { PropsWithChildren, ReactNode } from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView, useSafeAreaInsets } from 'react-native-safe-area-context';
import { FLOATING_TAB_CONTENT_INSET } from '@/components/floating-tab-bar';

import { colors, fonts, radius, typography } from '@/constants/theme';
import { AppRefreshControl } from '@/components/app-refresh-control';
import { ScreenEntrance } from '@/components/motion';
import { RefreshStatus } from '@/components/refresh-status';
import { useResponsiveLayout } from '@/hooks/use-responsive-layout';

type PageProps = PropsWithChildren<{
  title: string;
  description?: string;
  action?: ReactNode;
  scroll?: boolean;
  refreshing?: boolean;
  onRefresh?: () => void;
  onClose?: () => void;
  closeLabel?: string;
  bottomInset?: 'tab' | 'safe';
}>;

export function Page({ title, description, children, action, scroll = true, refreshing = false, onRefresh, onClose, closeLabel = '닫기', bottomInset = 'safe' }: PageProps) {
  const { contentWidth, gutter, isCompact } = useResponsiveLayout();
  const insets = useSafeAreaInsets();
  const bottomPadding = bottomInset === 'tab' ? FLOATING_TAB_CONTENT_INSET : Math.max(24, insets.bottom + 12);
  const head = <View style={[styles.fixedHead, { width: contentWidth, paddingHorizontal: gutter }]}><View style={{ flex: 1 }}><Text accessibilityRole="header" style={[styles.title, isCompact && styles.titleCompact]}>{title}</Text>{description ? <Text style={styles.description}>{description}</Text> : null}</View>{action}</View>;
  const body = <View style={[styles.content, { width: contentWidth, paddingHorizontal: gutter }]}>{children}</View>;
  return <SafeAreaView style={styles.safe}><View style={styles.navigation}><Pressable accessibilityLabel="뒤로 가기" hitSlop={6} onPress={() => router.back()} style={styles.back}><Ionicons name="chevron-back" size={23} color={colors.ink900} /></Pressable>{onClose ? <Pressable accessibilityLabel={closeLabel} hitSlop={6} onPress={onClose} style={styles.close}><Ionicons name="close" size={23} color={colors.ink900}/></Pressable> : null}</View>{head}<RefreshStatus visible={refreshing}/><ScreenEntrance>{scroll ? <ScrollView alwaysBounceVertical={Boolean(onRefresh)} contentContainerStyle={[styles.scroll, { paddingBottom: bottomPadding }]} refreshControl={onRefresh ? <AppRefreshControl refreshing={refreshing} onRefresh={onRefresh}/> : undefined} showsVerticalScrollIndicator={false}>{body}</ScrollView> : body}</ScreenEntrance></SafeAreaView>;
}

export function Panel({ children, tone = 'plain' }: PropsWithChildren<{ tone?: 'plain' | 'green' | 'yellow' }>) {
  return <View style={[styles.panel, tone === 'green' && styles.green, tone === 'yellow' && styles.yellow]}>{children}</View>;
}

export function Row({ label, value, strong }: { label: string; value: string; strong?: boolean }) {
  return <View style={styles.row}><Text style={styles.rowLabel}>{label}</Text><Text style={[styles.rowValue, strong && styles.strong]}>{value}</Text></View>;
}

export function PrimaryButton({ label, onPress, disabled }: { label: string; onPress?: () => void; disabled?: boolean }) {
  return <Pressable accessibilityRole="button" accessibilityState={{ disabled }} disabled={disabled} onPress={onPress} style={({ pressed }) => [styles.button, (pressed || disabled) && styles.pressed]}><Text style={styles.buttonText}>{label}</Text></Pressable>;
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.canvas },
  navigation: { minHeight: 44, paddingHorizontal: 12, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  back: { width: 44, height: 44, alignItems: 'center', justifyContent: 'center' },
  close: { width: 44, height: 44, alignItems: 'center', justifyContent: 'center' },
  scroll: {},
  content: { alignSelf: 'center', gap: 14 },
  fixedHead: { alignSelf: 'center', flexDirection: 'row', alignItems: 'flex-start', paddingTop: 6, paddingBottom: 8 },
  title: { ...typography.screenTitle, color: colors.ink900, fontFamily: fonts.body },
  titleCompact: { fontSize: 25, lineHeight: 32 },
  description: { ...typography.body, marginTop: 7, color: colors.ink700, fontFamily: fonts.body },
  panel: { padding: 17, backgroundColor: colors.white, borderRadius: radius.card, borderWidth: 1, borderColor: colors.line },
  green: { backgroundColor: colors.green50, borderColor: colors.green100 },
  yellow: { backgroundColor: '#FFF5D8', borderColor: '#F4DFA4' },
  row: { minHeight: 43, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: colors.line },
  rowLabel: { color: colors.ink700 },
  rowValue: { maxWidth: '62%', textAlign: 'right', color: colors.ink900, fontWeight: '700' },
  strong: { fontSize: 17, fontWeight: '700', fontFamily: fonts.body },
  button: { minHeight: 52, alignItems: 'center', justifyContent: 'center', borderRadius: radius.input, backgroundColor: colors.green300 },
  buttonText: { color: colors.white, fontSize: 16, fontWeight: '800', fontFamily: fonts.body },
  pressed: { opacity: 0.65, transform: [{ scale: 0.99 }] },
});
