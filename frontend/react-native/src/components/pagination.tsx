import { Ionicons } from '@expo/vector-icons';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { colors, fonts, radius } from '@/constants/theme';

export function Pagination({ page, totalPages, onChange }: { page: number; totalPages: number; onChange: (page: number) => void }) {
  if (totalPages <= 1) return null;
  const previousDisabled = page <= 0;
  const nextDisabled = page >= totalPages - 1;
  return <View accessibilityRole="adjustable" accessibilityLabel={`${totalPages}페이지 중 ${page + 1}페이지`} style={styles.root}><Pressable accessibilityLabel="이전 페이지" disabled={previousDisabled} onPress={() => onChange(page - 1)} style={({ pressed }) => [styles.button, (pressed || previousDisabled) && styles.disabled]}><Ionicons name="chevron-back" size={17} color={colors.ink900}/><Text style={styles.buttonText}>이전</Text></Pressable><Text style={styles.page}><Text style={styles.current}>{page + 1}</Text> / {totalPages}</Text><Pressable accessibilityLabel="다음 페이지" disabled={nextDisabled} onPress={() => onChange(page + 1)} style={({ pressed }) => [styles.button, (pressed || nextDisabled) && styles.disabled]}><Text style={styles.buttonText}>다음</Text><Ionicons name="chevron-forward" size={17} color={colors.ink900}/></Pressable></View>;
}

const styles = StyleSheet.create({
  root: { minHeight: 48, marginTop: 8, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  button: { minWidth: 82, minHeight: 44, paddingHorizontal: 12, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 3, borderRadius: radius.control, backgroundColor: colors.white, borderWidth: 1, borderColor: colors.line },
  buttonText: { color: colors.ink900, fontFamily: fonts.body, fontSize: 11, fontWeight: '800' },
  disabled: { opacity: 0.35 },
  page: { color: colors.ink500, fontFamily: fonts.body, fontSize: 11, fontWeight: '700' },
  current: { color: colors.ink900, fontWeight: '900' },
});
