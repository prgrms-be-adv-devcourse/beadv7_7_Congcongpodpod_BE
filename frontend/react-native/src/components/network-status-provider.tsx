import NetInfo from '@react-native-community/netinfo';
import { Ionicons } from '@expo/vector-icons';
import type { PropsWithChildren } from 'react';
import { useEffect, useRef, useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { colors, fonts, shadow } from '@/constants/theme';

export function NetworkStatusProvider({ children }: PropsWithChildren) {
  const insets = useSafeAreaInsets();
  const [offline, setOffline] = useState(false);
  const [reconnected, setReconnected] = useState(false);
  const wasOffline = useRef(false);

  useEffect(() => NetInfo.addEventListener(state => {
    const nextOffline = state.isConnected === false || state.isInternetReachable === false;
    if (wasOffline.current && !nextOffline) {
      setReconnected(true);
      setTimeout(() => setReconnected(false), 2200);
    }
    wasOffline.current = nextOffline;
    setOffline(nextOffline);
  }), []);

  return <View style={styles.root}>{children}{offline || reconnected ? <View accessibilityLiveRegion="assertive" accessibilityRole="alert" pointerEvents="none" style={[styles.banner, reconnected && styles.onlineBanner, { top: insets.top + 6 }]}><Ionicons name={offline ? 'cloud-offline-outline' : 'checkmark-circle-outline'} size={16} color={colors.white}/><Text style={styles.text}>{offline ? '네트워크 연결이 필요합니다' : '네트워크에 다시 연결됐습니다'}</Text></View> : null}</View>;
}

const styles = StyleSheet.create({
  root: { flex: 1 },
  banner: { position: 'absolute', zIndex: 1000, alignSelf: 'center', minHeight: 38, paddingHorizontal: 15, flexDirection: 'row', alignItems: 'center', gap: 7, borderRadius: 19, backgroundColor: colors.ink900, ...shadow.float },
  onlineBanner: { backgroundColor: colors.green700 },
  text: { color: colors.white, fontFamily: fonts.body, fontSize: 12, fontWeight: '800' },
});
