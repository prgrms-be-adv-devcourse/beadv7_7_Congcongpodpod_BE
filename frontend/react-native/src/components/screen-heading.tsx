import { StyleSheet, Text, View } from 'react-native';
import { colors, fonts } from '@/constants/theme';
import { useResponsiveLayout } from '@/hooks/use-responsive-layout';

export function ScreenHeading({ title, description }: { title: string; description?: string }) {
  const { contentWidth, gutter, isCompact } = useResponsiveLayout();
  return <View style={[styles.wrap, { width: contentWidth, paddingHorizontal: gutter }]}><Text style={[styles.title, isCompact && styles.titleCompact]}>{title}</Text>{description && <Text style={styles.description}>{description}</Text>}</View>;
}

const styles = StyleSheet.create({
  wrap: { alignSelf: 'center', paddingTop: 22, paddingBottom: 16 },
  title: { fontSize: 28, lineHeight: 35, letterSpacing: -1.2, fontWeight: '800', color: colors.ink900, fontFamily: fonts.body },
  titleCompact: { fontSize: 25, lineHeight: 32 },
  description: { maxWidth: 540, marginTop: 6, color: colors.ink700, fontSize: 14, lineHeight: 21, fontFamily: fonts.body },
});
