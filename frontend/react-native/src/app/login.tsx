import { RoundedIcon as Ionicons } from '@/components/rounded-icon';
import * as Crypto from 'expo-crypto';
import { router, useLocalSearchParams, type Href } from 'expo-router';
import { useEffect, useRef, useState } from 'react';
import { Platform, Pressable, StyleSheet, Text, TextInput, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { KakaoLoginSheet } from '@/components/kakao-login-sheet';
import { colors, radius } from '@/constants/theme';
import { useAuth } from '@/providers/auth-provider';

export default function LoginScreen() {
  const { redirect } = useLocalSearchParams<{ redirect?: string }>();
  const { signIn, signInWithKakao } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [kakaoUrl, setKakaoUrl] = useState<string>();
  const [kakaoState, setKakaoState] = useState<string>();
  const kakaoCompleting = useRef(false);
  const completeKakaoLoginRef = useRef<(url: string) => Promise<void>>(undefined);

  const goNext = () => {
    const next = typeof redirect === 'string' && redirect.startsWith('/') && !redirect.startsWith('//') ? redirect : '/my';
    router.replace(next as Href);
  };

  const submit = async () => {
    if (!email.trim() || !password) {
      setError('이메일과 비밀번호를 입력해 주세요.');
      return;
    }
    setLoading(true);
    setError(null);
    try {
      await signIn(email.trim(), password);
      goNext();
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : '로그인에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const startKakaoLogin = async () => {
    const clientId = process.env.EXPO_PUBLIC_KAKAO_REST_API_KEY?.trim();
    const redirectUri = Platform.OS === 'web'
      ? process.env.EXPO_PUBLIC_KAKAO_WEB_REDIRECT_URI?.trim() || `${window.location.origin}/login`
      : process.env.EXPO_PUBLIC_KAKAO_REDIRECT_URI?.trim();
    if (!clientId || !redirectUri) {
      setError('카카오 로그인 설정이 필요해요. REST API 키와 Redirect URI를 확인해주세요.');
      return;
    }
    try {
      const bytes = await Crypto.getRandomBytesAsync(24);
      const state = Array.from(bytes, value => value.toString(16).padStart(2, '0')).join('');
      const url = `https://kauth.kakao.com/oauth/authorize?client_id=${encodeURIComponent(clientId)}&redirect_uri=${encodeURIComponent(redirectUri)}&response_type=code&state=${state}`;
      kakaoCompleting.current = false;
      setError(null);
      setKakaoState(state);
      if (Platform.OS === 'web') window.sessionStorage.setItem('lastdish.kakaoState', state);
      setKakaoUrl(url);
    } catch {
      setError('안전한 로그인 요청을 만들지 못했어요. 앱을 다시 실행해주세요.');
    }
  };

  const completeKakaoLogin = async (url: string) => {
    const redirectUri = Platform.OS === 'web'
      ? process.env.EXPO_PUBLIC_KAKAO_WEB_REDIRECT_URI?.trim() || `${window.location.origin}/login`
      : process.env.EXPO_PUBLIC_KAKAO_REDIRECT_URI?.trim();
    if (!redirectUri || kakaoCompleting.current) return;
    const callback = new URL(url);
    const expected = new URL(redirectUri);
    if (callback.origin !== expected.origin || callback.pathname !== expected.pathname) return;
    kakaoCompleting.current = true;
    setKakaoUrl(undefined);
    const returnedState = callback.searchParams.get('state');
    const code = callback.searchParams.get('code');
    const kakaoError = callback.searchParams.get('error_description');
    const expectedState = kakaoState || (Platform.OS === 'web' ? window.sessionStorage.getItem('lastdish.kakaoState') : null);
    if (!expectedState || returnedState !== expectedState) {
      setError('로그인 요청을 확인하지 못했어요. 다시 시도해주세요.');
      kakaoCompleting.current = false;
      return;
    }
    if (!code) {
      setError(kakaoError || '카카오 로그인이 취소됐어요.');
      kakaoCompleting.current = false;
      return;
    }
    setLoading(true);
    setError(null);
    try {
      await signInWithKakao(code);
      if (Platform.OS === 'web') window.sessionStorage.removeItem('lastdish.kakaoState');
      goNext();
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : '카카오 로그인에 실패했습니다.');
    } finally {
      setLoading(false);
      kakaoCompleting.current = false;
      setKakaoState(undefined);
    }
  };
  completeKakaoLoginRef.current = completeKakaoLogin;

  useEffect(() => {
    if (Platform.OS !== 'web') return;
    const callback = new URL(window.location.href);
    if (callback.searchParams.has('code') || callback.searchParams.has('error')) void completeKakaoLoginRef.current?.(callback.toString());
  }, []);

  return (
    <SafeAreaView style={styles.safe}>
      <Ionicons name="chevron-back" size={27} onPress={() => router.back()} />
      <View style={styles.intro}>
        <Text style={styles.title}>남은 맛을, 좋은 가격에</Text>
        <Text style={styles.subtitle}>가까운 마감 할인 음식을 예약하고 픽업하세요.</Text>
      </View>
      <Pressable accessibilityRole="button" disabled={loading} onPress={() => void startKakaoLogin()} style={({ pressed }) => [styles.kakao, (pressed || loading) && styles.pressed]}>
        <View style={styles.kakaoMark}><Text style={styles.kakaoMarkText}>K</Text></View>
        <Text style={styles.kakaoText}>카카오로 로그인</Text>
      </Pressable>
      <View style={styles.divider}><View style={styles.line} /><Text style={styles.or}>또는 이메일로 로그인</Text><View style={styles.line} /></View>
      <TextInput value={email} onChangeText={setEmail} placeholder="이메일" keyboardType="email-address" autoCapitalize="none" autoCorrect={false} style={styles.input} />
      <TextInput value={password} onChangeText={setPassword} placeholder="비밀번호" secureTextEntry onSubmitEditing={() => void submit()} style={styles.input} />
      {error && <Text accessibilityRole="alert" style={styles.error}>{error}</Text>}
      <Pressable disabled={loading} onPress={() => void submit()} style={({ pressed }) => [styles.login, (pressed || loading) && styles.pressed]}>
        <Text style={styles.loginText}>{loading ? '로그인 중…' : '이메일로 로그인'}</Text>
      </Pressable>
      <Text onPress={() => router.push('/signup')} style={styles.signup}>계정이 없나요? 회원가입</Text>
      <KakaoLoginSheet url={kakaoUrl} onClose={() => setKakaoUrl(undefined)} onNavigate={url => { const redirectUri=process.env.EXPO_PUBLIC_KAKAO_REDIRECT_URI?.trim();if(!redirectUri)return true;try{const callback=new URL(url);const expected=new URL(redirectUri);if(callback.origin===expected.origin&&callback.pathname===expected.pathname){void completeKakaoLogin(url);return false}}catch{return true}return true; }} onHttpError={() => setError('카카오 로그인 화면을 불러오지 못했어요.')}/>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, paddingHorizontal: 22, paddingTop: 12, backgroundColor: colors.canvas },
  intro: { marginTop: 42, marginBottom: 30 },
  title: { fontSize: 27, fontWeight: '900', letterSpacing: -0.8, color: colors.ink900 },
  subtitle: { marginTop: 9, color: colors.ink700 },
  kakao: { flexDirection: 'row', justifyContent: 'center', alignItems: 'center', gap: 12, paddingVertical: 16, borderRadius: radius.input, backgroundColor: colors.kakao },
  kakaoMark: { width: 22, height: 22, alignItems: 'center', justifyContent: 'center', borderRadius: 11, backgroundColor: colors.ink900 },
  kakaoMarkText: { color: colors.kakao, fontSize: 11, fontWeight: '900' },
  kakaoText: { fontWeight: '900', color: colors.ink900 },
  divider: { flexDirection: 'row', alignItems: 'center', gap: 12, marginVertical: 24 },
  line: { flex: 1, height: 1, backgroundColor: colors.line },
  or: { color: colors.ink400, fontSize: 13 },
  input: { height: 58, marginBottom: 10, paddingHorizontal: 16, borderWidth: 1, borderColor: colors.line, borderRadius: radius.input, backgroundColor: colors.white, fontSize: 16 },
  error: { marginBottom: 8, color: '#B42318', fontSize: 13, fontWeight: '700' },
  login: { minHeight: 52, marginTop: 8, alignItems: 'center', justifyContent: 'center', borderRadius: radius.input, backgroundColor: colors.green300 },
  pressed: { opacity: 0.66 },
  loginText: { fontWeight: '900', color: colors.white },
  signup: { marginTop: 24, textAlign: 'center', color: colors.green700, fontWeight: '700' },
  kakaoSheet: { flex: 1, backgroundColor: colors.white },
  kakaoHeader: { height: 54, paddingHorizontal: 14, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', borderBottomWidth: 1, borderBottomColor: colors.line },
  kakaoTitle: { color: colors.ink900, fontSize: 15, fontWeight: '900' },
  close: { width: 40, height: 40, alignItems: 'center', justifyContent: 'center' },
  webLoading: { ...StyleSheet.absoluteFillObject, zIndex: 1, alignItems: 'center', justifyContent: 'center', gap: 12, backgroundColor: colors.white },
  webLoadingText: { color: colors.ink700, fontSize: 13, fontWeight: '700' },
});
