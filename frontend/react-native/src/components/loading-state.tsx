import { useEffect, useRef } from 'react';
import { Animated, StyleSheet, Text, View } from 'react-native';

import { BrandLogo } from '@/components/brand-logo';
import { colors, fonts, motion } from '@/constants/theme';
import { useReducedMotion } from '@/hooks/use-reduced-motion';
import { beginGlobalLoading, endGlobalLoading } from '@/lib/app-overlay';

export function LoadingState({ label = '맛있는 한 끼를 찾고 있어요', compact = false, inline = false }: { label?: string; compact?: boolean; inline?: boolean }) {
  const reduced = useReducedMotion();
  const pulse = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    if (inline) return;
    beginGlobalLoading();
    return endGlobalLoading;
  }, [inline]);

  useEffect(() => {
    if (reduced) return;
    const loop = Animated.loop(Animated.sequence([
      Animated.timing(pulse, { toValue: 1, duration: motion.emphasis, useNativeDriver: true }),
      Animated.timing(pulse, { toValue: 0, duration: motion.emphasis, useNativeDriver: true }),
    ]));
    loop.start();
    return () => loop.stop();
  }, [pulse, reduced]);

  const content = <View accessibilityLiveRegion="polite" accessibilityRole="progressbar" style={[styles.card, compact && styles.compact]}>
      <Animated.View style={{ opacity: reduced ? 1 : pulse.interpolate({ inputRange: [0, 1], outputRange: [0.82, 1] }), transform: [{ scale: reduced ? 1 : pulse.interpolate({ inputRange: [0, 1], outputRange: [0.985, 1.015] }) }] }}>
        <BrandLogo size={compact ? 44 : 64} />
      </Animated.View>
      {!compact ? <Text style={styles.brand}>라디</Text> : null}
      <Text style={styles.label}>{label}</Text>
      <View style={styles.dots}>{[0, 1, 2].map((dot) => <View key={dot} style={[styles.dot, dot === 1 && styles.dotStrong]} />)}</View>
    </View>;
  if (inline) return content;
  return null;
}

const styles = StyleSheet.create({
  card: { width: 210, minHeight: 190, alignItems: 'center', justifyContent: 'center', padding: 24, borderRadius: 16, backgroundColor: colors.white },
  compact: { width: 184, minHeight: 142, padding: 12 },
  brand: { marginTop: 2, color: colors.ink900, fontFamily: fonts.body, fontSize: 15, fontWeight: '900', letterSpacing: -0.4 },
  label: { marginTop: 3, color: colors.ink700, fontFamily: fonts.body, fontSize: 13, fontWeight: '700' },
  dots: { marginTop: 10, flexDirection: 'row', gap: 5 },
  dot: { width: 5, height: 5, borderRadius: 3, backgroundColor: colors.green200 },
  dotStrong: { backgroundColor: colors.green500 },
});
