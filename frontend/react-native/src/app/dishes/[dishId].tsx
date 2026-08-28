import { RoundedIcon as Ionicons } from '@/components/rounded-icon';
import { router, useLocalSearchParams } from 'expo-router';
import { useCallback, useEffect, useState } from 'react';
import { LayoutAnimation, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView, useSafeAreaInsets } from 'react-native-safe-area-context';
import { OptimizedImage as Image } from '@/components/optimized-image';

import { EmptyState } from '@/components/empty-state';
import { AppRefreshControl } from '@/components/app-refresh-control';
import { CartQuantityBadge } from '@/components/cart-quantity-badge';
import { ConfirmModal } from '@/components/confirm-modal';
import { showAppAlert } from '@/lib/app-overlay';
import { LoadingState } from '@/components/loading-state';
import { ScreenEntrance } from '@/components/motion';
import { RefreshStatus } from '@/components/refresh-status';
import { colors, fonts, radius } from '@/constants/theme';
import { useResponsiveLayout } from '@/hooks/use-responsive-layout';
import { usePullToRefresh } from '@/hooks/use-pull-to-refresh';
import { getDishImageSource } from '@/lib/food-image';
import { showLoginRequired } from '@/lib/login-required';
import { getStoreCategoryVisual } from '@/lib/store-category';
import { formatDishPickupWindow } from '@/lib/store-pricing';
import { getDish, getStore } from '@/lib/stores';
import { useAuth } from '@/providers/auth-provider';
import { useCart } from '@/providers/cart-provider';
import type { Dish, Store } from '@/types/store';

type DetailOrigin = '/' | '/stores' | '/favorites' | '/orders' | '/my';
const detailOrigins = new Set<DetailOrigin>(['/', '/stores', '/favorites', '/orders', '/my']);

export default function DishDetail() {
  const params = useLocalSearchParams<{ dishId: string; storeId?: string; storeName?: string; category?: string; origin?: string }>();
  const insets = useSafeAreaInsets();
  const { contentWidth } = useResponsiveLayout();
  const [dish, setDish] = useState<Dish | null>(null);
  const [store, setStore] = useState<Store | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [replaceConfirming, setReplaceConfirming] = useState(false);
  const { member } = useAuth();
  const { item: cartItem, add, clear } = useCart();
  const origin: DetailOrigin = detailOrigins.has(params.origin as DetailOrigin) ? params.origin as DetailOrigin : '/';
  const closeToOrigin = () => {
    if (origin === '/stores') return router.replace('/stores');
    if (origin === '/favorites') return router.replace('/favorites');
    if (origin === '/orders') return router.replace('/orders');
    if (origin === '/my') return router.replace('/my');
    return router.replace('/');
  };

  const load = useCallback(async () => {
    setLoading(true);
    setError(false);
    try {
      const nextDish = await getDish(Number(params.dishId));
      setDish(nextDish);
      const nextStoreId = Number(params.storeId ?? nextDish.storeId ?? 0);
      if (nextStoreId) {
        try {
          setStore(await getStore(nextStoreId));
        } catch {
          setStore(null);
        }
      }
    } catch {
      setError(true);
    } finally {
      setLoading(false);
    }
  }, [params.dishId, params.storeId]);

  useEffect(() => { void load(); }, [load]);
  const { refreshing, onRefresh } = usePullToRefresh(load);

  if (loading && !dish) return <SafeAreaView style={styles.state}><Pressable accessibilityLabel="뒤로 가기" onPress={() => router.back()} style={styles.stateBack}><Ionicons name="chevron-back" size={24} color={colors.ink900}/></Pressable><Pressable accessibilityLabel="상품 상세 닫기" onPress={closeToOrigin} style={styles.stateClose}><Ionicons name="close" size={24} color={colors.ink900}/></Pressable><LoadingState label="오늘의 상품을 꺼내고 있어요" /></SafeAreaView>;
  if (error || !dish) return <SafeAreaView style={styles.state}><Pressable accessibilityLabel="뒤로 가기" onPress={() => router.back()} style={styles.stateBack}><Ionicons name="chevron-back" size={24} color={colors.ink900} /></Pressable><Pressable accessibilityLabel="상품 상세 닫기" onPress={closeToOrigin} style={styles.stateClose}><Ionicons name="close" size={24} color={colors.ink900}/></Pressable><EmptyState title="상품을 불러오지 못했어요" description="판매 상태가 바뀌었거나 잠시 연결이 불안정해요." actionLabel="다시 시도" onAction={() => void load()} /></SafeAreaView>;

  const storeName = store?.storeName ?? params.storeName ?? dish.storeName ?? '매장';
  const rawStoreId = store?.storeId ?? Number(params.storeId ?? dish.storeId ?? 0);
  const storeId = rawStoreId || undefined;
  const category = store?.category ?? params.category;
  const categoryLabel = category ? getStoreCategoryVisual(category).label : '마감 할인';
  const discountRate = dish.price > dish.discountPrice ? Math.round((1 - dish.discountPrice / dish.price) * 100) : 0;
  const savedPrice = Math.max(0, dish.price - dish.discountPrice);
  const soldOut = dish.quantity <= 0;
  const cartQuantity = cartItem?.dishId === dish.dishId ? cartItem.cartQuantity : 0;
  const atStockLimit = !soldOut && cartQuantity >= dish.quantity;
  const primaryLabel = soldOut ? '품절된 상품이에요' : atStockLimit ? `재고 ${dish.quantity}개를 모두 담았어요` : !member ? '로그인 후 담기' : cartQuantity ? '1개 더 담기' : `${dish.discountPrice.toLocaleString()}원 담기`;
  const pickupTime = formatDishPickupWindow(dish);
  const heroHeight = contentWidth * 3 / 4;
  const commitAdd = async () => {
    try {
      await add({ ...dish, storeCategory: category }, storeName, storeId);
      LayoutAnimation.configureNext({ duration: 220, update: { type: LayoutAnimation.Types.easeInEaseOut }, create: { type: LayoutAnimation.Types.easeInEaseOut, property: LayoutAnimation.Properties.opacity } });
      setReplaceConfirming(false);
    } catch (addError) {
      showAppAlert('장바구니에 담지 못했어요', addError instanceof Error ? addError.message : '잠시 후 다시 시도해주세요.');
    }
  };
  const handlePrimary = () => {
    if (!member) {
      showLoginRequired(`/dishes/${params.dishId}`);
      return;
    }
    const differentStore = cartItem && ((cartItem.storeId && storeId && cartItem.storeId !== storeId) || ((!cartItem.storeId || !storeId) && cartItem.storeName !== storeName));
    if (differentStore) {
      setReplaceConfirming(true);
      return;
    }
    void commitAdd();
  };

  return <SafeAreaView style={styles.safe} edges={['bottom']}>
    <RefreshStatus visible={refreshing}/>
    <ScreenEntrance><ScrollView alwaysBounceVertical contentContainerStyle={styles.scroll} refreshControl={<AppRefreshControl refreshing={refreshing} onRefresh={onRefresh}/>} showsVerticalScrollIndicator={false}>
      <View style={[styles.hero, { height: heroHeight, maxWidth: contentWidth }]}>
        <Image accessibilityLabel={`${dish.dishName} 상품 이미지`} source={getDishImageSource(dish, category)} style={StyleSheet.absoluteFillObject} resizeMode="cover" />
        <View style={styles.heroShade}/>
        <Pressable accessibilityLabel="뒤로 가기" onPress={() => router.back()} style={[styles.back, { top: insets.top + 8 }]}><Ionicons name="chevron-back" size={24} color={colors.ink900} /></Pressable>
        <Pressable accessibilityLabel="상품 상세 닫기" onPress={closeToOrigin} style={[styles.close, { top: insets.top + 8 }]}><Ionicons name="close" size={24} color={colors.ink900}/></Pressable>
        <View accessibilityLabel={soldOut ? '품절' : `남은 수량 ${dish.quantity}개`} style={[styles.stock, soldOut && styles.stockSoldOut]}>{soldOut ? <Text style={styles.stockSoldOutText}>품절</Text> : <><Text style={styles.stockLabel}>남은 수량</Text><View style={styles.stockDivider}/><Text style={styles.stockValue}>{dish.quantity}<Text style={styles.stockUnit}>개</Text></Text></>}</View>
      </View>
      <View style={[styles.content, { maxWidth: contentWidth }]}>
        <Pressable accessibilityRole="button" disabled={!storeId} onPress={() => storeId && router.push({ pathname: '/stores/[storeId]', params: { storeId: String(storeId), origin } })} style={styles.storeRow}>
          <View><Text style={styles.storeName}>{storeName}</Text><Text style={styles.storeCategory}>{categoryLabel}</Text></View>
          {storeId ? <View style={styles.storeLinkWrap}><Text style={styles.storeLink}>매장 보기</Text><Ionicons name="chevron-forward" size={16} color={colors.ink700}/></View> : null}
        </Pressable>
        <View style={styles.info}>
          <Text style={styles.saleLabel}>오늘 마감 할인</Text>
          <Text accessibilityRole="header" style={styles.title}>{dish.dishName}</Text>
          <Text style={styles.description}>{dish.description || '오늘 준비한 상품을 좋은 가격에 픽업하세요.'}</Text>
          <View style={styles.priceBlock}>
            {dish.price > 0 && dish.price !== dish.discountPrice ? <Text style={styles.original}>{dish.price.toLocaleString()}원</Text> : null}
            <View style={styles.priceRow}>{discountRate > 0 ? <Text style={styles.discount}>{discountRate}%</Text> : null}<Text style={styles.price}>{dish.discountPrice.toLocaleString()}원</Text></View>
            {savedPrice > 0 ? <Text style={styles.saving}>{savedPrice.toLocaleString()}원 아껴요</Text> : null}
          </View>
          <View style={styles.pickup}><View style={styles.pickupIcon}><Ionicons name="time-outline" size={18} color={colors.green700} /></View><View style={styles.pickupCopy}><Text style={styles.pickupLabel}>픽업 가능 시간</Text><Text style={styles.pickupValue}>{pickupTime}</Text></View><Ionicons name="bag-handle-outline" size={19} color={colors.ink400}/></View>
          <View style={styles.guide}><Text style={styles.guideTitle}>상품 안내</Text><Text style={styles.guideText}>마감 할인 상품은 매일 구성이 달라질 수 있어요. 알레르기 재료는 매장에 확인해주세요.</Text></View>
        </View>
      </View>
    </ScrollView></ScreenEntrance>
    <View style={styles.footer}><View style={[styles.footerActions, { maxWidth: contentWidth }]}><Pressable accessibilityRole="button" accessibilityHint={!member ? '로그인 화면으로 이동합니다' : undefined} accessibilityState={{ disabled: soldOut || atStockLimit }} disabled={soldOut || atStockLimit} onPress={handlePrimary} style={({ pressed }) => [styles.cartButton, (soldOut || atStockLimit) && styles.cartButtonDisabled, pressed && !soldOut && !atStockLimit && styles.pressed]}><Ionicons name={member ? 'cart-outline' : 'lock-closed-outline'} size={20} color={soldOut || atStockLimit ? colors.ink400 : colors.white} /><Text numberOfLines={1} style={[styles.cartButtonText, (soldOut || atStockLimit) && styles.cartButtonTextDisabled]}>{primaryLabel}</Text></Pressable>{cartQuantity > 0 ? <Pressable accessibilityRole="button" accessibilityLabel={`장바구니로 이동, 현재 ${cartQuantity}개`} onPress={() => router.push({ pathname: '/cart', params: { origin } })} style={({ pressed }) => [styles.goToCartButton, pressed && styles.pressed]}><CartQuantityBadge quantity={cartQuantity}/><Text style={styles.goToCartText}>장바구니 이동</Text><Ionicons name="arrow-forward" size={17} color={colors.white}/></Pressable> : null}</View></View>
    <ConfirmModal visible={replaceConfirming} icon="cart-outline" title="장바구니를 새로 담을까요?" description={`${cartItem?.storeName ?? '다른 매장'} 상품이 이미 담겨 있어요. 계속하면 기존 장바구니가 삭제됩니다.`} confirmLabel="기존 상품 삭제 후 담기" onCancel={()=>setReplaceConfirming(false)} onConfirm={()=>void (async()=>{try{await clear();await commitAdd();}catch(clearError){showAppAlert('장바구니를 비우지 못했어요',clearError instanceof Error?clearError.message:'잠시 후 다시 시도해주세요.');}})()}/>
  </SafeAreaView>;
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.white }, state: { flex: 1, justifyContent: 'center', backgroundColor: colors.canvas }, stateBack: { position: 'absolute', left: 14, top: 12, zIndex: 2, width: 44, height: 44, alignItems: 'center', justifyContent: 'center', borderRadius: 22, backgroundColor: colors.white, borderWidth: 1, borderColor: colors.line }, stateClose: { position: 'absolute', right: 14, top: 12, zIndex: 2, width: 44, height: 44, alignItems: 'center', justifyContent: 'center', borderRadius: 22, backgroundColor: colors.white, borderWidth: 1, borderColor: colors.line }, scroll: { paddingBottom: 30, backgroundColor: colors.white },
  hero: { width: '100%', alignSelf: 'center', overflow: 'hidden', backgroundColor: colors.canvas }, heroShade: { ...StyleSheet.absoluteFillObject, backgroundColor: 'rgba(0,0,0,0.04)' }, back: { position: 'absolute', left: 14, width: 44, height: 44, alignItems: 'center', justifyContent: 'center', borderRadius: 22, backgroundColor: 'rgba(255,255,255,0.94)' }, close: { position: 'absolute', right: 14, width: 44, height: 44, alignItems: 'center', justifyContent: 'center', borderRadius: 22, backgroundColor: 'rgba(255,255,255,0.94)' }, stock: { position: 'absolute', right: 16, bottom: 14, minHeight: 42, paddingHorizontal: 12, flexDirection: 'row', alignItems: 'center', gap: 9, borderRadius: radius.control, backgroundColor: 'rgba(23,26,24,0.9)' }, stockSoldOut: { minWidth: 68, justifyContent: 'center' }, stockLabel: { color: 'rgba(255,255,255,0.72)', fontFamily: fonts.body, fontSize: 10, fontWeight: '700' }, stockDivider: { width: 1, height: 18, backgroundColor: 'rgba(255,255,255,0.24)' }, stockValue: { color: colors.white, fontFamily: fonts.body, fontSize: 18, lineHeight: 22, fontWeight: '900', fontVariant: ['tabular-nums'] }, stockUnit: { fontSize: 11, fontWeight: '800' }, stockSoldOutText: { color: colors.white, fontFamily: fonts.body, fontSize: 13, fontWeight: '900', textAlign: 'center' },
  content: { width: '100%', alignSelf: 'center', paddingHorizontal: 20 }, storeRow: { minHeight: 64, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', borderBottomWidth: 1, borderBottomColor: colors.line }, storeName: { color: colors.ink900, fontFamily: fonts.body, fontSize: 14, fontWeight: '800' }, storeCategory: { marginTop: 3, color: colors.ink500, fontFamily: fonts.body, fontSize: 11 }, storeLinkWrap: { minHeight: 44, flexDirection: 'row', alignItems: 'center', gap: 2 }, storeLink: { color: colors.ink700, fontFamily: fonts.body, fontSize: 12, fontWeight: '700' },
  info: { paddingTop: 20 }, saleLabel: { color: colors.green700, fontFamily: fonts.body, fontSize: 12, fontWeight: '900' }, title: { marginTop: 6, color: colors.ink900, fontFamily: fonts.body, fontSize: 28, lineHeight: 35, fontWeight: '900', letterSpacing: -1 }, description: { marginTop: 9, color: colors.ink700, fontFamily: fonts.body, fontSize: 14, lineHeight: 21 }, priceBlock: { marginTop: 20 }, original: { color: colors.ink400, fontFamily: fonts.body, fontSize: 13, textDecorationLine: 'line-through', fontVariant: ['tabular-nums'] }, priceRow: { marginTop: 2, flexDirection: 'row', alignItems: 'baseline', gap: 8 }, discount: { color: colors.green700, fontFamily: fonts.body, fontSize: 23, fontWeight: '900', fontVariant: ['tabular-nums'] }, price: { color: colors.ink900, fontFamily: fonts.body, fontSize: 28, fontWeight: '900', letterSpacing: -0.7, fontVariant: ['tabular-nums'] }, saving: { marginTop: 4, color: colors.ink500, fontFamily: fonts.body, fontSize: 12, fontWeight: '600', fontVariant: ['tabular-nums'] },
  pickup: { minHeight: 72, marginTop: 22, paddingHorizontal: 14, flexDirection: 'row', alignItems: 'center', gap: 11, backgroundColor: colors.blue50, borderWidth: 1, borderColor: colors.blue300, borderRadius: radius.input }, pickupIcon: { width: 34, height: 34, alignItems: 'center', justifyContent: 'center', borderRadius: 17, backgroundColor: colors.white }, pickupCopy: { flex: 1 }, pickupLabel: { color: colors.ink500, fontFamily: fonts.body, fontSize: 11, fontWeight: '700' }, pickupValue: { marginTop: 3, color: colors.ink900, fontFamily: fonts.body, fontSize: 14, fontWeight: '800' }, guide: { marginTop: 24, paddingTop: 18, borderTopWidth: 1, borderTopColor: colors.line }, guideTitle: { color: colors.ink900, fontFamily: fonts.body, fontSize: 15, fontWeight: '800' }, guideText: { marginTop: 8, color: colors.ink500, fontFamily: fonts.body, fontSize: 12, lineHeight: 19 },
  footer: { paddingHorizontal: 14, paddingTop: 12, paddingBottom: 12, backgroundColor: colors.white, borderTopWidth: 1, borderTopColor: colors.line }, footerActions: { width: '100%', alignSelf: 'center', flexDirection: 'row', gap: 8 }, cartButton: { minWidth: 0, minHeight: 54, flex: 1, paddingHorizontal: 12, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 7, borderRadius: radius.input, backgroundColor: colors.green500 }, cartButtonDisabled: { backgroundColor: colors.line }, cartButtonText: { flexShrink: 1, color: colors.white, fontFamily: fonts.body, fontSize: 15, fontWeight: '900' }, cartButtonTextDisabled: { color: colors.ink400 }, goToCartButton: { minHeight: 54, paddingHorizontal: 13, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 5, borderRadius: radius.input, backgroundColor: colors.ink900 }, goToCartText: { color: colors.white, fontFamily: fonts.body, fontSize: 13, fontWeight: '900' }, pressed: { opacity: 0.76, transform: [{ scale: 0.992 }] },
});
