import { router } from 'expo-router';
import { useState } from 'react';
import { Pressable, StyleSheet, Text, TextInput } from 'react-native';
import { Page, PrimaryButton } from '@/components/page';
import { colors, radius } from '@/constants/theme';

export default function Signup() {
  const [values, setValues] = useState({ name: '', email: '', password: '', phone: '' });
  const field = (key: keyof typeof values, placeholder: string, secure = false) => <TextInput value={values[key]} onChangeText={(value) => setValues((current) => ({ ...current, [key]: value }))} placeholder={placeholder} secureTextEntry={secure} autoCapitalize="none" style={styles.input} />;
  return <Page title="회원가입" description="카카오 계정 또는 이메일로 LastDish를 시작하세요."><Pressable style={styles.kakao}><Text style={styles.kakaoText}>K　카카오로 시작하기</Text></Pressable><Text style={styles.or}>또는 이메일로 가입</Text>{field('name', '이름')}{field('email', '이메일')}{field('password', '비밀번호 · 8자 이상', true)}{field('phone', '전화번호 · 010-0000-0000')}<PrimaryButton label="이메일로 가입" onPress={() => router.replace('/login')} /></Page>;
}
const styles = StyleSheet.create({ kakao: { minHeight: 54, alignItems: 'center', justifyContent: 'center', borderRadius: radius.input, backgroundColor: colors.kakao }, kakaoText: { color: colors.ink900, fontWeight: '900' }, or: { textAlign: 'center', color: colors.ink400, fontSize: 13 }, input: { height: 58, paddingHorizontal: 15, borderWidth: 1, borderColor: colors.line, borderRadius: radius.input, backgroundColor: colors.white, fontSize: 15 } });
