import { Ionicons } from '@expo/vector-icons';
import * as Haptics from 'expo-haptics';
import { router } from 'expo-router';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Animated, Image, PanResponder, Pressable, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { colors, fonts, motion, radius } from '@/constants/theme';
import { CartQuantityBadge } from '@/components/cart-quantity-badge';
import { useReducedMotion } from '@/hooks/use-reduced-motion';
import { useResponsiveLayout } from '@/hooks/use-responsive-layout';
import { getDishImageSource } from '@/lib/food-image';
import { useCart } from '@/providers/cart-provider';

type CartOrigin = '/' | '/stores' | '/favorites' | '/orders' | '/my';
const OPEN_CART_THRESHOLD_Y = -18;
const MAX_OVERSCROLL_Y = -34;

export function CartSummarySheet({ origin, bottomOffset = 0, compact = false, actionLabel, actionDisabled = false, onAction }: { origin: CartOrigin; bottomOffset?: number; compact?: boolean; actionLabel?: string; actionDisabled?: boolean; onAction?: () => void }) {
  const { item } = useCart();
  const { contentWidth } = useResponsiveLayout();
  const insets = useSafeAreaInsets();
  const reducedMotion = useReducedMotion();
  const translateY = useRef(new Animated.Value(0)).current;
  const dragStart = useRef(0);
  const [expanded, setExpanded] = useState(true);
  const hasAction = compact && Boolean(actionLabel && onAction);
  const safeBottom = Math.max(insets.bottom, 20);
  const sheetHeight = (hasAction ? 174 : compact ? 110 : 140) + safeBottom;
  const collapsedY = hasAction ? 130 : compact ? 66 : 96;

  const moveTo = useCallback((open: boolean) => {
    setExpanded(open);
    if (reducedMotion) {
      translateY.setValue(open ? 0 : collapsedY);
      return;
    }
    void Haptics.selectionAsync();
    Animated.spring(translateY, { toValue: open ? 0 : collapsedY, damping: 25, stiffness: 280, mass: 0.82, useNativeDriver: true }).start();
  }, [collapsedY, reducedMotion, translateY]);

  useEffect(() => { if (item) moveTo(true); }, [item, moveTo]);

  const openCart = useCallback(() => {
    if (reducedMotion) {
      router.push({ pathname: '/cart', params: { origin } });
      return;
    }
    void Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Medium);
    Animated.timing(translateY, { toValue: -44, duration: motion.fast, useNativeDriver: true }).start(() => {
      translateY.setValue(0);
      router.push({ pathname: '/cart', params: { origin } });
    });
  }, [origin, reducedMotion, translateY]);

  const panResponder = useMemo(() => PanResponder.create({
    onMoveShouldSetPanResponder: (_, gesture) => Math.abs(gesture.dy) > 5 && Math.abs(gesture.dy) > Math.abs(gesture.dx),
    onPanResponderGrant: () => translateY.stopAnimation((value) => { dragStart.current = value; }),
    onPanResponderMove: (_, gesture) => {
      const next = Math.max(MAX_OVERSCROLL_Y, Math.min(collapsedY, dragStart.current + gesture.dy));
      translateY.setValue(next);
    },
    onPanResponderRelease: (_, gesture) => {
      const projected = dragStart.current + gesture.dy + gesture.vy * 35;
      if (projected <= OPEN_CART_THRESHOLD_Y) {
        openCart();
        return;
      }
      moveTo(projected < collapsedY / 2);
    },
    onPanResponderTerminate: () => {
      translateY.stopAnimation((value) => moveTo(value < collapsedY / 2));
    },
  }), [collapsedY, moveTo, openCart, translateY]);

  if (!item) return null;
  const total = item.discountPrice * item.cartQuantity;

  return <View pointerEvents="box-none" style={[styles.stage, { bottom: bottomOffset }]}><Animated.View accessibilityLabel={`장바구니 요약, ${item.dishName} ${item.cartQuantity}개`} style={[styles.sheet, compact && styles.compactSheet, { width: contentWidth, height: sheetHeight, paddingBottom: safeBottom, transform: [{ translateY }] }]}> 
    <View
      accessibilityActions={[{ name: expanded ? 'collapse' : 'expand', label: expanded ? '장바구니 요약 내리기' : '장바구니 요약 올리기' }]}
      accessibilityHint={expanded ? '손잡이를 아래로 끌어 접거나 위로 당겨 장바구니를 엽니다' : '손잡이를 위로 끌어 요약을 펼칩니다'}
      accessibilityRole="adjustable"
      onAccessibilityAction={() => moveTo(!expanded)}
      style={styles.handleArea}
      {...panResponder.panHandlers}>
      <View style={styles.handle}/>
    </View>
    <View style={[styles.summary, compact && styles.compactSummary]}>
      {!compact && <Image source={getDishImageSource(item, item.storeCategory)} style={styles.image}/>} 
      <View style={styles.copy}>{compact ? <><Text style={styles.compactCount}>담긴 상품 {item.cartQuantity}개</Text><Text style={styles.compactTotal}>{total.toLocaleString()}원</Text></> : <><Text numberOfLines={1} style={styles.store}>{item.storeName}</Text><Text numberOfLines={1} style={styles.dish}>{item.dishName} · {item.cartQuantity}개</Text><Text style={styles.total}>{total.toLocaleString()}원</Text></>}</View>
      <Pressable accessibilityRole="button" onPress={() => router.push({ pathname: '/cart', params: { origin } })} style={({ pressed }) => [styles.shortcut, compact && styles.compactShortcut, pressed && styles.pressed]}><CartQuantityBadge quantity={item.cartQuantity}/><Text style={styles.shortcutText}>장바구니 보기</Text><Ionicons name="arrow-forward" size={16} color={colors.white}/></Pressable>
    </View>
    {hasAction ? <View style={styles.sheetActionWrap}><Pressable accessibilityRole="button" accessibilityState={{ disabled: actionDisabled }} disabled={actionDisabled} onPress={onAction} style={({ pressed }) => [styles.sheetAction, actionDisabled && styles.sheetActionDisabled, pressed && !actionDisabled && styles.pressed]}><Ionicons name="cart-outline" size={20} color={actionDisabled ? colors.ink400 : colors.white}/><Text style={[styles.sheetActionText, actionDisabled && styles.sheetActionTextDisabled]}>{actionLabel}</Text></Pressable></View> : null}
  </Animated.View></View>;
}

const styles = StyleSheet.create({
  stage: { position: 'absolute', left: 0, right: 0, zIndex: 30, alignItems: 'center' },
  sheet: {
    borderTopLeftRadius: 28,
    borderTopRightRadius: 28,
    backgroundColor: colors.white,
    borderTopWidth: 1,
    borderColor: colors.lineStrong,
    shadowColor: colors.ink900,
    shadowOffset: { width: 0, height: -8 },
    shadowOpacity: 0.14,
    shadowRadius: 18,
    elevation: 18,
  },
  compactSheet: { backgroundColor: colors.white },
  handleArea: { height: 22, paddingTop: 8, alignItems: 'center', borderTopLeftRadius: 28, borderTopRightRadius: 28, backgroundColor: colors.white },
  handle: { width: 38, height: 4, borderRadius: 2, backgroundColor: colors.lineStrong },
  summary: { height: 88, paddingHorizontal: 12, paddingBottom: 9, flexDirection: 'row', alignItems: 'center', gap: 10 },
  compactSummary: { height: 66, paddingHorizontal: 16, paddingBottom: 10 },
  image: { width: 54, height: 54, borderRadius: 10, backgroundColor: colors.white, borderWidth: 1, borderColor: colors.line },
  copy: { flex: 1, minWidth: 0 },
  store: { color: colors.ink500, fontFamily: fonts.body, fontSize: 10, fontWeight: '700' },
  dish: { marginTop: 3, color: colors.ink900, fontFamily: fonts.body, fontSize: 13, fontWeight: '900' },
  total: { marginTop: 4, color: colors.ink900, fontFamily: fonts.body, fontSize: 14, fontWeight: '900' },
  compactCount: { color: colors.ink500, fontFamily: fonts.body, fontSize: 10, fontWeight: '700' },
  compactTotal: { marginTop: 2, color: colors.ink900, fontFamily: fonts.body, fontSize: 20, fontWeight: '900', letterSpacing: -0.4 },
  shortcut: { minHeight: 44, paddingHorizontal: 13, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 5, borderRadius: radius.control, backgroundColor: colors.green500 },
  compactShortcut: { paddingHorizontal: 15 },
  shortcutText: { color: colors.white, fontFamily: fonts.body, fontSize: 12, fontWeight: '900' },
  sheetActionWrap: { height: 64, paddingHorizontal: 14, paddingBottom: 10, backgroundColor: colors.white },
  sheetAction: { minHeight: 54, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 8, borderRadius: radius.input, backgroundColor: colors.green500 },
  sheetActionDisabled: { backgroundColor: colors.line },
  sheetActionText: { color: colors.white, fontFamily: fonts.body, fontSize: 16, fontWeight: '900' },
  sheetActionTextDisabled: { color: colors.ink400 },
  pressed: { opacity: 0.72, transform: [{ scale: 0.99 }] },
});
