import { StyleSheet, Text, View } from 'react-native';
import { colors, fonts, typography } from '@/constants/theme';
import { useResponsiveLayout } from '@/hooks/use-responsive-layout';

export function ScreenHeading({ title, description }: { title: string; description?: string }) {
  const { contentWidth, gutter, isCompact } = useResponsiveLayout();
  return <View style={[styles.wrap, { width: contentWidth, paddingHorizontal: gutter }]}><Text accessibilityRole="header" style={[styles.title, isCompact && styles.titleCompact]}>{title}</Text>{description && <Text style={styles.description}>{description}</Text>}</View>;
}

const styles = StyleSheet.create({
  wrap: { alignSelf: 'center', paddingTop: 22, paddingBottom: 16 },
  title: { ...typography.screenTitle, color: colors.ink900, fontFamily: fonts.body },
  titleCompact: { fontSize: 25, lineHeight: 32 },
  description: { ...typography.body, maxWidth: 540, marginTop: 6, color: colors.ink700, fontFamily: fonts.body },
});
