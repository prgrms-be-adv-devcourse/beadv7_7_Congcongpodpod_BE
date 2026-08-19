import { Ionicons } from '@expo/vector-icons';
import * as Haptics from 'expo-haptics';
import { type ComponentProps, useEffect, useRef } from 'react';
import { Animated } from 'react-native';

import { useReducedMotion } from '@/hooks/use-reduced-motion';

type IconName = ComponentProps<typeof Ionicons>['name'];

const tabPressListeners = new Map<string, Set<() => void>>();

export function triggerTabFeedback(tabKey: string) {
  void Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light).catch(() => undefined);
  tabPressListeners.get(tabKey)?.forEach((listener) => listener());
}

export function AnimatedTabIcon({ tabKey, color, focused, idle, active, size }: { tabKey: string; color: string; focused: boolean; idle: IconName; active: IconName; size: number }) {
  const reducedMotion = useReducedMotion();
  const scale = useRef(new Animated.Value(1)).current;

  useEffect(() => {
    const listeners = tabPressListeners.get(tabKey) ?? new Set<() => void>();
    const animate = () => {
      if (reducedMotion) return;
      scale.stopAnimation();
      scale.setValue(1);
      Animated.sequence([
        Animated.timing(scale, { toValue: 0.84, duration: 65, useNativeDriver: true }),
        Animated.spring(scale, {
          toValue: 1,
          stiffness: 430,
          damping: 12,
          mass: 0.42,
          useNativeDriver: true,
        }),
      ]).start();
    };
    listeners.add(animate);
    tabPressListeners.set(tabKey, listeners);
    return () => {
      listeners.delete(animate);
      if (!listeners.size) tabPressListeners.delete(tabKey);
      scale.stopAnimation();
    };
  }, [reducedMotion, scale, tabKey]);

  return <Animated.View style={{ transform: [{ scale }] }}><Ionicons name={focused ? active : idle} size={size} color={color} /></Animated.View>;
}
