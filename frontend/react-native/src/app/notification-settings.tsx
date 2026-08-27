import { RoundedIcon as Ionicons } from '@/components/rounded-icon';
import { useEffect, useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { Page, Panel } from '@/components/page';
import { colors, fonts, radius } from '@/constants/theme';
import { DEFAULT_NOTIFICATION_PREFERENCES, loadNotificationPreferences, saveNotificationPreferences, type NotificationPreferences } from '@/lib/notification-preferences';

type PreferenceKey = Exclude<keyof NotificationPreferences, 'enabled'>;

export default function NotificationSettingsScreen() {
  const [preferences, setPreferences] = useState(DEFAULT_NOTIFICATION_PREFERENCES);

  useEffect(() => { void loadNotificationPreferences().then(setPreferences); }, []);

  const update = (next: NotificationPreferences) => {
    setPreferences(next);
    void saveNotificationPreferences(next);
  };

  const toggle = (key: PreferenceKey, value: boolean) => update({ ...preferences, [key]: value });

  return <Page title="알림 설정" description="앱에서 바로 표시할 알림을 선택하세요.">
    <Panel>
      <SettingRow icon="notifications-outline" title="인앱 팝업 알림" description="앱 사용 중 화면 상단에 알림을 표시해요." value={preferences.enabled} onValueChange={(enabled) => update({ ...preferences, enabled })} last/>
    </Panel>
    <View style={styles.sectionHead}><Text style={styles.sectionTitle}>알림 유형</Text><Text style={styles.sectionDescription}>꺼도 알림센터의 기록은 유지돼요.</Text></View>
    <View style={[styles.group, !preferences.enabled && styles.disabled]}>
      <SettingRow icon="receipt-outline" title="주문 상태" description="접수·취소 등 주문 변경 안내" value={preferences.orders} disabled={!preferences.enabled} onValueChange={(value) => toggle('orders', value)}/>
      <SettingRow icon="bag-check-outline" title="픽업 안내" description="픽업 준비와 완료 안내" value={preferences.pickup} disabled={!preferences.enabled} onValueChange={(value) => toggle('pickup', value)}/>
      <SettingRow icon="pricetag-outline" title="마감 할인·혜택" description="주변 할인과 라디 혜택 소식" value={preferences.benefits} disabled={!preferences.enabled} onValueChange={(value) => toggle('benefits', value)} last/>
    </View>
  </Page>;
}

function SettingRow({ icon, title, description, value, disabled, last, onValueChange }: { icon: keyof typeof Ionicons.glyphMap; title: string; description: string; value: boolean; disabled?: boolean; last?: boolean; onValueChange: (value: boolean) => void }) {
  return <View style={[styles.row, !last && styles.rowBorder]}><View style={styles.icon}><Ionicons name={icon} size={19} color={disabled ? colors.ink400 : colors.ink700}/></View><View style={styles.copy}><Text style={styles.title}>{title}</Text><Text style={styles.description}>{description}</Text></View><SettingSwitch label={title} disabled={disabled} value={value} onValueChange={onValueChange}/></View>;
}

function SettingSwitch({ label, disabled, value, onValueChange }: { label: string; disabled?: boolean; value: boolean; onValueChange: (value: boolean) => void }) {
  return <Pressable accessibilityLabel={label} accessibilityRole="switch" accessibilityState={{ checked: value, disabled }} disabled={disabled} hitSlop={4} onPress={() => onValueChange(!value)} style={styles.switchTarget}><View style={[styles.switchTrack, value && styles.switchTrackActive]}><View style={[styles.switchThumb, value && styles.switchThumbActive]}/></View></Pressable>;
}

const styles = StyleSheet.create({
  sectionHead: { marginTop: 10, gap: 3 },
  sectionTitle: { color: colors.ink900, fontFamily: fonts.body, fontSize: 16, fontWeight: '900' },
  sectionDescription: { color: colors.ink500, fontFamily: fonts.body, fontSize: 12 },
  group: { overflow: 'hidden', borderRadius: radius.card, borderWidth: 1, borderColor: colors.line, backgroundColor: colors.white },
  disabled: { opacity: 0.55 },
  row: { minHeight: 78, paddingHorizontal: 15, flexDirection: 'row', alignItems: 'center', gap: 12 },
  rowBorder: { borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: colors.line },
  icon: { width: 38, height: 38, alignItems: 'center', justifyContent: 'center', borderRadius: 12, backgroundColor: colors.canvas },
  copy: { flex: 1, minWidth: 0 },
  switchTarget: { width: 52, height: 44, flexShrink: 0, alignItems: 'center', justifyContent: 'center' },
  switchTrack: { width: 44, height: 26, padding: 3, justifyContent: 'center', borderRadius: 13, backgroundColor: colors.lineStrong },
  switchTrackActive: { backgroundColor: colors.green500 },
  switchThumb: { width: 20, height: 20, borderRadius: 10, backgroundColor: colors.white },
  switchThumbActive: { alignSelf: 'flex-end' },
  title: { color: colors.ink900, fontFamily: fonts.body, fontSize: 14, fontWeight: '800' },
  description: { marginTop: 3, color: colors.ink500, fontFamily: fonts.body, fontSize: 11, lineHeight: 16 },
});
