import { Ionicons } from '@expo/vector-icons';
import type { PropsWithChildren } from 'react';
import { useEffect, useRef, useState } from 'react';
import { Modal, Pressable, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { LoadingState } from '@/components/loading-state';
import { colors, fonts, radius, shadow } from '@/constants/theme';
import { type AppAlertRequest, subscribeAppAlerts, subscribeGlobalLoading } from '@/lib/app-overlay';

const MIN_LOADING_VISIBLE_MS = 350;

export function AppOverlayProvider({ children }: PropsWithChildren) {
  const insets = useSafeAreaInsets();
  const [alert, setAlert] = useState<AppAlertRequest>();
  const [loadingCount, setLoadingCount] = useState(0);
  const [showLoading, setShowLoading] = useState(false);
  const loadingVisible = useRef(false);
  const loadingShownAt = useRef(0);
  const hideLoadingTimer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);

  useEffect(() => subscribeAppAlerts(setAlert), []);
  useEffect(() => subscribeGlobalLoading(setLoadingCount), []);
  useEffect(() => {
    if (hideLoadingTimer.current) clearTimeout(hideLoadingTimer.current);
    if (loadingCount > 0) {
      if (!loadingVisible.current) {
        loadingVisible.current = true;
        loadingShownAt.current = Date.now();
        setShowLoading(true);
      }
      return;
    }
    if (!loadingVisible.current) return;
    const remaining = Math.max(0, MIN_LOADING_VISIBLE_MS - (Date.now() - loadingShownAt.current));
    hideLoadingTimer.current = setTimeout(() => {
      loadingVisible.current = false;
      setShowLoading(false);
    }, remaining);
  }, [loadingCount]);
  useEffect(() => () => { if (hideLoadingTimer.current) clearTimeout(hideLoadingTimer.current); }, []);

  const dismiss = () => {
    const cancel = alert?.buttons.find(button => button.style === 'cancel');
    setAlert(undefined);
    cancel?.onPress?.();
  };

  return <>
    {children}
    <Modal animationType="fade" onRequestClose={dismiss} presentationStyle="overFullScreen" transparent visible={Boolean(alert)}>
      <View style={[styles.root, { paddingBottom: Math.max(20, insets.bottom) }]}>
        <Pressable accessibilityLabel="알림 닫기" onPress={dismiss} style={styles.scrim}/>
        {alert ? <View accessibilityRole="alert" accessibilityViewIsModal style={styles.card}>
          <View style={[styles.icon, alert.buttons.some(button => button.style === 'destructive') && styles.iconDanger]}><Ionicons name={alert.buttons.some(button => button.style === 'destructive') ? 'alert-circle-outline' : 'notifications-outline'} size={21} color={alert.buttons.some(button => button.style === 'destructive') ? colors.danger700 : colors.green700}/></View>
          <Text style={styles.title}>{alert.title}</Text>
          {alert.message ? <Text style={styles.message}>{alert.message}</Text> : null}
          <View style={styles.actions}>{alert.buttons.map((button, index) => <Pressable accessibilityRole="button" key={`${alert.id}-${index}`} onPress={() => { setAlert(undefined); button.onPress?.(); }} style={({ pressed }) => [styles.action, button.style === 'cancel' && styles.cancel, button.style === 'destructive' && styles.danger, pressed && styles.pressed]}><Text style={[styles.actionText, button.style === 'cancel' && styles.cancelText]}>{button.text ?? '확인'}</Text></Pressable>)}</View>
        </View> : null}
      </View>
    </Modal>
    <Modal animationType="fade" presentationStyle="overFullScreen" transparent visible={showLoading}>
      <View accessibilityLabel="처리 중" accessibilityRole="progressbar" style={styles.loadingRoot}><View style={styles.loadingCard}><LoadingState compact inline label="잠시만 기다려주세요"/></View></View>
    </Modal>
  </>;
}

const styles = StyleSheet.create({
  root: { flex: 1, alignItems: 'center', justifyContent: 'center', paddingHorizontal: 24 },
  scrim: { ...StyleSheet.absoluteFillObject, backgroundColor: 'rgba(15,20,17,0.48)' },
  card: { width: '100%', maxWidth: 360, paddingHorizontal: 20, paddingTop: 24, paddingBottom: 18, alignItems: 'center', borderRadius: radius.card, backgroundColor: colors.white, ...shadow.float },
  icon: { width: 44, height: 44, alignItems: 'center', justifyContent: 'center', borderRadius: 22, backgroundColor: colors.green50 },
  iconDanger: { backgroundColor: colors.danger50 },
  title: { marginTop: 15, color: colors.ink900, fontFamily: fonts.body, fontSize: 20, lineHeight: 27, fontWeight: '900', letterSpacing: -0.5, textAlign: 'center' },
  message: { marginTop: 7, color: colors.ink700, fontFamily: fonts.body, fontSize: 13, lineHeight: 20, textAlign: 'center' },
  actions: { width: '100%', marginTop: 22, flexDirection: 'row', gap: 8 },
  action: { minHeight: 50, flex: 1, alignItems: 'center', justifyContent: 'center', borderRadius: radius.input, backgroundColor: colors.green500 },
  cancel: { borderWidth: 1, borderColor: colors.lineStrong, backgroundColor: colors.white },
  danger: { backgroundColor: colors.danger700 },
  actionText: { color: colors.white, fontFamily: fonts.body, fontSize: 14, fontWeight: '900' },
  cancelText: { color: colors.ink900 },
  pressed: { opacity: 0.72, transform: [{ scale: 0.985 }] },
  loadingRoot: { flex: 1, alignItems: 'center', justifyContent: 'center', paddingHorizontal: 24, backgroundColor: 'rgba(15,20,17,0.34)' },
  loadingCard: { width: 184, minHeight: 142, alignItems: 'center', justifyContent: 'center', borderRadius: radius.card, backgroundColor: colors.white, ...shadow.float },
});
