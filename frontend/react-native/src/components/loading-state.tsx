import { useEffect, useRef } from 'react';
import { Animated, Easing, StyleSheet, Text, View } from 'react-native';

import { colors, fonts } from '@/constants/theme';
import { useReducedMotion } from '@/hooks/use-reduced-motion';

export function LoadingState({ label = '맛있는 한 끼를 찾고 있어요', compact = false, inline = false }: { label?: string; compact?: boolean; inline?: boolean }) {
  const reducedMotion = useReducedMotion();
  const rotation = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    if (reducedMotion) return;
    const animation = Animated.loop(Animated.timing(rotation, { toValue: 1, duration: 760, easing: Easing.linear, useNativeDriver: true }));
    animation.start();
    return () => animation.stop();
  }, [reducedMotion, rotation]);

  const content = <View accessibilityLiveRegion="polite" accessibilityRole="progressbar" style={[styles.card, compact && styles.compact]}>
      <Animated.View style={[styles.spinner, compact && styles.spinnerCompact, { transform: [{ rotate: reducedMotion ? '0deg' : rotation.interpolate({ inputRange: [0, 1], outputRange: ['0deg', '360deg'] }) }] }]}/>
      <Text style={styles.label}>{label}</Text>
    </View>;
  if (inline) return content;
  return <View style={styles.stage}>{content}</View>;
}

const styles = StyleSheet.create({
  stage: { flex: 1, minHeight: 150, alignItems: 'center', justifyContent: 'center' },
  card: { minWidth: 152, minHeight: 104, alignItems: 'center', justifyContent: 'center', padding: 16 },
  compact: { minWidth: 136, minHeight: 86, padding: 12 },
  spinner: { width: 38, height: 38, borderRadius: 19, borderWidth: 8, borderColor: colors.green500, borderTopColor: colors.ink900 },
  spinnerCompact: { width: 30, height: 30, borderRadius: 15, borderWidth: 6 },
  label: { marginTop: 10, color: colors.ink700, fontFamily: fonts.body, fontSize: 12, lineHeight: 17, fontWeight: '700', textAlign: 'center' },
});
