import { RoundedIcon as Ionicons } from '@/components/rounded-icon';
import { router } from 'expo-router';
import { useEffect, useState } from 'react';
import { Modal, Pressable, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { colors, fonts, radius, shadow, typography } from '@/constants/theme';
import { subscribeLoginRequired, type LoginRequiredRequest } from '@/lib/login-required';

export function LoginRequiredModal() {
  const [request, setRequest] = useState<LoginRequiredRequest>();
  const insets = useSafeAreaInsets();

  useEffect(() => subscribeLoginRequired(setRequest), []);

  const cancel = () => {
    const onCancel = request?.onCancel;
    setRequest(undefined);
    onCancel?.();
  };
  const login = () => {
    if (!request) return;
    const redirect = request.redirect;
    setRequest(undefined);
    router.push({ pathname: '/login', params: { redirect } });
  };

  return (
    <Modal animationType="fade" onRequestClose={cancel} presentationStyle="overFullScreen" transparent visible={Boolean(request)}>
      <View style={[styles.root, { paddingBottom: Math.max(20, insets.bottom) }]}>
        <Pressable accessibilityLabel="로그인 안내 닫기" onPress={cancel} style={styles.scrim}/>
        <View accessibilityRole="alert" accessibilityViewIsModal style={styles.card}>
          <View style={styles.icon}><Ionicons name="lock-closed" size={21} color={colors.green700}/></View>
          <Text style={styles.title}>로그인이 필요해요</Text>
          <Text style={styles.description}>로그인해야 이용할 수 있습니다.</Text>
          <View style={styles.actions}>
            <Pressable accessibilityRole="button" onPress={cancel} style={({ pressed }) => [styles.cancel, pressed && styles.pressed]}><Text style={styles.cancelText}>취소</Text></Pressable>
            <Pressable accessibilityRole="button" onPress={login} style={({ pressed }) => [styles.login, pressed && styles.pressed]}><Text style={styles.loginText}>로그인하기</Text></Pressable>
          </View>
        </View>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, alignItems: 'center', justifyContent: 'center', paddingHorizontal: 24 },
  scrim: { ...StyleSheet.absoluteFillObject, backgroundColor: 'rgba(15, 20, 17, 0.48)' },
  card: { width: '100%', maxWidth: 360, paddingHorizontal: 20, paddingTop: 24, paddingBottom: 18, alignItems: 'center', borderRadius: radius.card, borderWidth: StyleSheet.hairlineWidth, borderColor: colors.lineStrong, backgroundColor: colors.white, ...shadow.sheet },
  icon: { width: 44, height: 44, alignItems: 'center', justifyContent: 'center', borderRadius: 22, backgroundColor: colors.green50 },
  title: { ...typography.sectionTitle, marginTop: 15, color: colors.ink900, fontFamily: fonts.body },
  description: { marginTop: 6, color: colors.ink700, fontFamily: fonts.body, fontSize: 13, lineHeight: 20, textAlign: 'center' },
  actions: { width: '100%', marginTop: 22, flexDirection: 'row', gap: 8 },
  cancel: { minHeight: 50, flex: 0.8, alignItems: 'center', justifyContent: 'center', borderRadius: radius.input, borderWidth: 1, borderColor: colors.lineStrong, backgroundColor: colors.white },
  login: { minHeight: 50, flex: 1.2, alignItems: 'center', justifyContent: 'center', borderRadius: radius.input, backgroundColor: colors.green500 },
  cancelText: { color: colors.ink900, fontFamily: fonts.body, fontSize: 14, fontWeight: '800' },
  loginText: { color: colors.white, fontFamily: fonts.body, fontSize: 14, fontWeight: '900' },
  pressed: { opacity: 0.72, transform: [{ scale: 0.985 }] },
});
