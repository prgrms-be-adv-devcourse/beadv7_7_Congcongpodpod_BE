import type { PropsWithChildren } from 'react';
import { useEffect, useRef, useState } from 'react';
import { Animated, Easing, StyleSheet, type StyleProp, type ViewStyle } from 'react-native';

import { motion } from '@/constants/theme';
import { useReducedMotion } from '@/hooks/use-reduced-motion';

export function ScreenEntrance({ children, style }: PropsWithChildren<{ style?: StyleProp<ViewStyle> }>) {
  const reduced = useReducedMotion();
  const progress = useRef(new Animated.Value(reduced ? 1 : 0)).current;

  useEffect(() => {
    if (reduced) {
      progress.setValue(1);
      return;
    }
    Animated.timing(progress, { toValue: 1, duration: motion.screen, useNativeDriver: true }).start();
  }, [progress, reduced]);

  return (
    <Animated.View style={[styles.fill, style, { opacity: progress, transform: [{ translateY: progress.interpolate({ inputRange: [0, 1], outputRange: [7, 0] }) }] }]}>
      {children}
    </Animated.View>
  );
}

export function FloatingEntrance({ visible, children, style }: PropsWithChildren<{ visible: boolean; style?: StyleProp<ViewStyle> }>) {
  const reduced = useReducedMotion();
  const progress = useRef(new Animated.Value(visible ? 1 : 0)).current;
  const cachedChildren = useRef(children);
  const [mounted, setMounted] = useState(visible);

  if (visible) cachedChildren.current = children;

  useEffect(() => {
    if (visible) setMounted(true);
    Animated.timing(progress, {
      toValue: visible ? 1 : 0,
      duration: reduced ? 0 : visible ? motion.screen : motion.base,
      easing: Easing.bezier(0.22, 1, 0.36, 1),
      useNativeDriver: true,
    }).start(({ finished }) => {
      if (finished && !visible) setMounted(false);
    });
  }, [progress, reduced, visible]);

  if (!mounted && !visible) return null;
  return <Animated.View pointerEvents={visible ? 'auto' : 'none'} style={[style, { opacity: progress, transform: [{ translateY: progress.interpolate({ inputRange: [0, 1], outputRange: [24, 0] }) }] }]}>{cachedChildren.current}</Animated.View>;
}

const styles = StyleSheet.create({ fill: { flex: 1 } });
