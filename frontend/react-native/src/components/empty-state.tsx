import { Pressable, StyleSheet, Text, View } from 'react-native';

import { BrandLogo } from '@/components/brand-logo';
import { colors, fonts, radius } from '@/constants/theme';

export function EmptyState({ title, description, actionLabel, onAction }: { title: string; description?: string; actionLabel?: string; onAction?: () => void }) {
  return <View style={styles.wrap}><BrandLogo size={58}/><Text style={styles.title}>{title}</Text>{description ? <Text style={styles.description}>{description}</Text> : null}{actionLabel && onAction ? <Pressable accessibilityRole="button" onPress={onAction} style={({ pressed }) => [styles.action, pressed && styles.pressed]}><Text style={styles.actionText}>{actionLabel}</Text></Pressable> : null}</View>;
}

const styles = StyleSheet.create({
  wrap: { minHeight: 250, paddingHorizontal: 24, alignItems: 'center', justifyContent: 'center' },
  title: { marginTop: 4, color: colors.ink900, fontFamily: fonts.body, fontSize: 18, fontWeight: '800', letterSpacing: -0.45 },
  description: { maxWidth: 310, marginTop: 7, color: colors.ink700, fontFamily: fonts.body, fontSize: 13, lineHeight: 20, textAlign: 'center' },
  action: { minHeight: 44, marginTop: 16, paddingHorizontal: 18, alignItems: 'center', justifyContent: 'center', borderRadius: radius.control, backgroundColor: colors.green500 },
  actionText: { color: colors.white, fontFamily: fonts.body, fontSize: 13, fontWeight: '800' },
  pressed: { opacity: 0.7, transform: [{ scale: 0.985 }] },
});
