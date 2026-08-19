import { Ionicons } from '@expo/vector-icons';
import { Modal, Pressable, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { colors, fonts, radius, shadow } from '@/constants/theme';

export function ConfirmModal({ visible, icon, title, description, confirmLabel, busy = false, busyLabel = '변경 중…', tone = 'default', onCancel, onConfirm }: { visible: boolean; icon: keyof typeof Ionicons.glyphMap; title: string; description: string; confirmLabel: string; busy?: boolean; busyLabel?: string; tone?: 'default' | 'danger'; onCancel: () => void; onConfirm: () => void }) {
  const insets = useSafeAreaInsets();
  const danger = tone === 'danger';
  return <Modal animationType="fade" onRequestClose={onCancel} presentationStyle="overFullScreen" transparent visible={visible}><View style={[styles.root, { paddingBottom: Math.max(20, insets.bottom) }]}><Pressable accessibilityLabel="확인 팝업 닫기" disabled={busy} onPress={onCancel} style={styles.scrim}/><View accessibilityRole="alert" accessibilityViewIsModal style={styles.card}><View style={[styles.icon, danger && styles.iconDanger]}><Ionicons name={icon} size={21} color={danger ? colors.danger700 : colors.green700}/></View><Text style={styles.title}>{title}</Text><Text style={styles.description}>{description}</Text><View style={styles.actions}><Pressable accessibilityRole="button" disabled={busy} onPress={onCancel} style={({ pressed }) => [styles.cancel, (pressed || busy) && styles.pressed]}><Text style={styles.cancelText}>취소</Text></Pressable><Pressable accessibilityRole="button" disabled={busy} onPress={onConfirm} style={({ pressed }) => [styles.confirm, danger && styles.confirmDanger, (pressed || busy) && styles.pressed]}><Text style={styles.confirmText}>{busy ? busyLabel : confirmLabel}</Text></Pressable></View></View></View></Modal>;
}

const styles = StyleSheet.create({
  root: { flex: 1, alignItems: 'center', justifyContent: 'center', paddingHorizontal: 24 },
  scrim: { ...StyleSheet.absoluteFillObject, backgroundColor: 'rgba(15, 20, 17, 0.48)' },
  card: { width: '100%', maxWidth: 360, paddingHorizontal: 20, paddingTop: 24, paddingBottom: 18, alignItems: 'center', borderRadius: radius.card, backgroundColor: colors.white, ...shadow.float },
  icon: { width: 44, height: 44, alignItems: 'center', justifyContent: 'center', borderRadius: 22, backgroundColor: colors.green50 },
  iconDanger: { backgroundColor: colors.danger50 },
  title: { marginTop: 15, color: colors.ink900, fontFamily: fonts.body, fontSize: 20, lineHeight: 27, fontWeight: '900', letterSpacing: -0.5 },
  description: { marginTop: 6, color: colors.ink700, fontFamily: fonts.body, fontSize: 13, lineHeight: 20, textAlign: 'center' },
  actions: { width: '100%', marginTop: 22, flexDirection: 'row', gap: 8 },
  cancel: { minHeight: 50, flex: 0.8, alignItems: 'center', justifyContent: 'center', borderRadius: radius.input, borderWidth: 1, borderColor: colors.lineStrong, backgroundColor: colors.white },
  confirm: { minHeight: 50, flex: 1.2, alignItems: 'center', justifyContent: 'center', borderRadius: radius.input, backgroundColor: colors.green500 },
  confirmDanger: { backgroundColor: colors.danger700 },
  cancelText: { color: colors.ink900, fontFamily: fonts.body, fontSize: 14, fontWeight: '800' },
  confirmText: { color: colors.white, fontFamily: fonts.body, fontSize: 14, fontWeight: '900' },
  pressed: { opacity: 0.72, transform: [{ scale: 0.985 }] },
});
