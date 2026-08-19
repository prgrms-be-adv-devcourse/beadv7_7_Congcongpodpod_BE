import { router } from 'expo-router';
import { useEffect, useState } from 'react';
import { StyleSheet, Text, TextInput } from 'react-native';

import { Page, PrimaryButton } from '@/components/page';
import { showAppAlert } from '@/lib/app-overlay';
import { colors, fonts, radius } from '@/constants/theme';
import { updateMyProfile } from '@/lib/account';
import { useAuth } from '@/providers/auth-provider';

export default function ProfileEditScreen() {
  const { member, refreshProfile } = useAuth();
  const [form, setForm] = useState({ userName: '', name: '', phone: '', email: '', password: '' });
  const [saving, setSaving] = useState(false);
  useEffect(() => { if (member) setForm({ userName: member.userName ?? '', name: member.name ?? '', phone: member.phone ?? '', email: member.email ?? '', password: '' }); }, [member]);
  const change = (key: keyof typeof form) => (value: string) => setForm((current) => ({ ...current, [key]: value }));
  const save = async () => { try { setSaving(true); await updateMyProfile({ ...form, password: form.password || undefined }); await refreshProfile(); showAppAlert('저장 완료', '내 정보가 변경됐어요.', [{ text: '확인', onPress: () => router.replace('/profile') }]); } catch (error) { showAppAlert('저장하지 못했어요', error instanceof Error ? error.message : '잠시 후 다시 시도해주세요.'); } finally { setSaving(false); } };

  return <Page title="내 정보 수정" description="계정에 사용할 정보를 확인하고 변경하세요." onClose={() => router.replace('/my')} closeLabel="수정 닫기">
    <Field label="아이디" value={form.userName} onChangeText={change('userName')} autoCapitalize="none" />
    <Field label="이름" value={form.name} onChangeText={change('name')} />
    <Field label="이메일" value={form.email} onChangeText={change('email')} keyboardType="email-address" autoCapitalize="none" />
    <Field label="전화번호" value={form.phone} onChangeText={change('phone')} keyboardType="phone-pad" />
    <Field label="새 비밀번호" hint="변경할 때만 입력 · 8자 이상" value={form.password} onChangeText={change('password')} secureTextEntry />
    <PrimaryButton label={saving ? '저장 중…' : '변경사항 저장'} disabled={saving} onPress={() => void save()} />
  </Page>;
}

function Field({ label, hint, ...props }: { label: string; hint?: string } & React.ComponentProps<typeof TextInput>) { return <><Text style={styles.label}>{label}{hint ? <Text style={styles.hint}>  {hint}</Text> : null}</Text><TextInput placeholderTextColor={colors.ink400} style={styles.input} {...props} /></>; }
const styles = StyleSheet.create({ label: { marginBottom: -7, color: colors.ink900, fontFamily: fonts.body, fontSize: 13, fontWeight: '700' }, hint: { color: colors.ink400, fontSize: 11, fontWeight: '400' }, input: { minHeight: 52, paddingHorizontal: 14, color: colors.ink900, fontFamily: fonts.body, fontSize: 14, backgroundColor: colors.white, borderWidth: 1, borderColor: colors.line, borderRadius: radius.input } });
