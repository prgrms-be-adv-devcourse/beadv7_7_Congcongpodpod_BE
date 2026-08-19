import { Ionicons } from '@expo/vector-icons';
import { router, useLocalSearchParams } from 'expo-router';
import { useCallback, useEffect, useState } from 'react';
import { Image, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView, useSafeAreaInsets } from 'react-native-safe-area-context';

import { EmptyState } from '@/components/empty-state';
import { AppRefreshControl } from '@/components/app-refresh-control';
import { CartSummarySheet } from '@/components/cart-summary-sheet';
import { MapCanvas } from '@/components/map-canvas';
import { LoadingState } from '@/components/loading-state';
import { RefreshStatus } from '@/components/refresh-status';
import { ScreenEntrance } from '@/components/motion';
import { colors, fonts, radius, shadow } from '@/constants/theme';
import { useResponsiveLayout } from '@/hooks/use-responsive-layout';
import { usePullToRefresh } from '@/hooks/use-pull-to-refresh';
import { getDishImageSource, getStoreCoverImageSource, getStoreProfileImageSource } from '@/lib/food-image';
import { getStoreCategoryVisual } from '@/lib/store-category';
import { getStore, getStoreDishes } from '@/lib/stores';
import { useCart } from '@/providers/cart-provider';
import type { Dish, Store } from '@/types/store';

type StoreOrigin = '/' | '/stores' | '/favorites' | '/orders' | '/my';
type StoreMapCommand = { id: number; type: 'focus'; latitude: number; longitude: number; zoom: number };
const storeOrigins = new Set<StoreOrigin>(['/', '/stores', '/favorites', '/orders', '/my']);

export default function StoreDetailScreen() {
  const params = useLocalSearchParams<{ storeId: string; origin?: string }>();
  const id = Number(params.storeId);
  const insets = useSafeAreaInsets();
  const { width, contentWidth } = useResponsiveLayout();
  const [store, setStore] = useState<Store | null>(null);
  const [dishes, setDishes] = useState<Dish[]>([]);
  const [favorite, setFavorite] = useState(false);
  const [loading, setLoading] = useState(true);
  const [failed, setFailed] = useState(false);
  const [mapCommand, setMapCommand] = useState<StoreMapCommand>();
  const { item: cartItem } = useCart();
  const origin: StoreOrigin = storeOrigins.has(params.origin as StoreOrigin) ? params.origin as StoreOrigin : '/';

  const load = useCallback(async () => {
    setLoading(true);
    setFailed(false);
    try {
      const [nextStore, nextDishes] = await Promise.all([getStore(id), getStoreDishes(id)]);
      setStore(nextStore);
      setDishes(nextDishes.length ? nextDishes : nextStore.dishes);
    } catch {
      setFailed(true);
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => { void load(); }, [load]);
  const refreshDishes = useCallback(async () => {
    const nextDishes = await getStoreDishes(id);
    setDishes(nextDishes);
  }, [id]);
  const { refreshing: dishesRefreshing, onRefresh: refreshDishesOnly } = usePullToRefresh(refreshDishes);

  const closeToOrigin = () => {
    if (origin === '/stores') return router.replace('/stores');
    if (origin === '/favorites') return router.replace('/favorites');
    if (origin === '/orders') return router.replace('/orders');
    if (origin === '/my') return router.replace('/my');
    return router.replace('/');
  };

  if (loading && !store) return <SafeAreaView style={styles.state}><Pressable accessibilityLabel={`${originLabel(origin)}으로 닫기`} onPress={closeToOrigin} style={styles.stateClose}><Ionicons name="close" size={24} color={colors.ink900}/></Pressable><LoadingState label="매장과 오늘의 메뉴를 준비하고 있어요" /></SafeAreaView>;
  if (failed || !store) return <SafeAreaView style={styles.state}><Pressable accessibilityLabel={`${originLabel(origin)}으로 닫기`} onPress={closeToOrigin} style={styles.stateClose}><Ionicons name="close" size={24} color={colors.ink900}/></Pressable><EmptyState title="매장 정보를 불러오지 못했어요" description="잠시 후 다시 확인해주세요." actionLabel="다시 시도" onAction={() => void load()} /></SafeAreaView>;

  const center = { latitude: store.latitude, longitude: store.longitude };
  const category = getStoreCategoryVisual(store.category);
  const heroHeight = Math.min(350, Math.max(280, width * 0.72));
  const pickupCutoff = store.closeTime?.slice(0, 5) ?? '오늘';
  const sameStoreCart = Boolean(cartItem && (cartItem.storeId === store.storeId || (!cartItem.storeId && cartItem.storeName === store.storeName)));

  return <SafeAreaView style={styles.safe} edges={['bottom']}>
    <ScreenEntrance><ScrollView alwaysBounceVertical contentContainerStyle={[styles.content, sameStoreCart && styles.contentWithCart]} refreshControl={<AppRefreshControl refreshing={dishesRefreshing} onRefresh={refreshDishesOnly}/>} showsVerticalScrollIndicator={false}>
      <View style={[styles.hero, { height: heroHeight, maxWidth: contentWidth }]}>
        <Image source={getStoreCoverImageSource(store)} style={StyleSheet.absoluteFillObject} resizeMode="cover" />
        <View style={styles.heroShade}/>
        <Pressable accessibilityLabel="뒤로 가기" onPress={() => router.back()} style={[styles.heroButton, { top: insets.top + 8 }]}><Ionicons name="chevron-back" size={24} color={colors.ink900} /></Pressable>
        <Pressable accessibilityLabel={`${originLabel(origin)}으로 닫기`} onPress={closeToOrigin} style={[styles.closeButton, { top: insets.top + 8 }]}><Ionicons name="close" size={24} color={colors.ink900} /></Pressable>
        <View style={styles.pickupBadge}><View style={styles.pickupDot}/><Text style={styles.pickupText}>오늘 {pickupCutoff}까지 픽업</Text></View>
      </View>
      <View style={[styles.body, { maxWidth: contentWidth }]}>
        <View style={styles.storeHead}>
          <Image accessibilityLabel={`${store.storeName} 프로필 이미지`} source={getStoreProfileImageSource(store)} style={styles.storeProfile}/>
          <View style={styles.storeCopy}><Text style={styles.name}>{store.storeName}</Text><View style={styles.facts}><Text style={styles.category}>{category.label}</Text><View style={styles.factDot}/><Text style={styles.fact}>픽업 가능</Text><View style={styles.factDot}/><Text style={styles.fact}>{pickupCutoff} 마감</Text></View></View>
          <Pressable accessibilityLabel={favorite ? '찜 해제' : '찜하기'} accessibilityState={{ selected: favorite }} onPress={() => setFavorite((value) => !value)} style={({ pressed }) => [styles.favorite, pressed && styles.pressed]}><Ionicons name={favorite ? 'heart' : 'heart-outline'} size={22} color={favorite ? colors.green700 : colors.ink700} /></Pressable>
        </View>
        <View style={styles.notice}><View style={styles.noticeIcon}><Ionicons name="megaphone-outline" size={17} color={colors.warning}/></View><View style={styles.noticeCopy}><Text style={styles.noticeTitle}>사장님 알림</Text><Text style={styles.noticeBody}>매일 남는 구성이 달라요. 알레르기 재료는 픽업 전 확인해주세요.</Text></View></View>
        <View style={styles.locationHead}><Text style={styles.sectionTitle}>매장 위치</Text><Text style={styles.route}>길찾기</Text></View>
        <View style={styles.map}>
          <MapCanvas stores={[store]} center={center} cameraCommand={mapCommand} zoom={16} showUserLocation={false} selectedStoreId={store.storeId} onSelect={() => undefined} />
          <Pressable
            accessibilityLabel="매장 위치로 돌아가기"
            accessibilityHint="움직인 지도를 매장 위치로 다시 맞춥니다"
            onPress={() => setMapCommand({ id: Date.now(), type: 'focus', ...center, zoom: 16 })}
            style={({ pressed }) => [styles.mapRecenter, pressed && styles.mapRecenterPressed]}
          ><Ionicons name="storefront-outline" size={20} color={colors.green700}/></Pressable>
          <View style={styles.addressBadge}><Ionicons name="location-outline" size={14} color={colors.ink700}/><Text numberOfLines={1} style={styles.address}>{store.address || '주소 정보 없음'}</Text></View>
        </View>
        <View style={styles.menuHead}><View><Text style={styles.activeTab}>마감 할인 상품</Text><Text style={styles.menuDescription}>아래로 당겨 오늘의 상품을 새로고침하세요</Text></View><Text style={styles.menuCount}>{dishes.length}개</Text></View>
        <RefreshStatus visible={dishesRefreshing}/>
        {dishes.length ? <View style={styles.dishGrid}>{dishes.map((dish) => <DishTile key={dish.dishId} dish={dish} store={store} origin={origin}/>)}</View> : <View style={styles.empty}><Ionicons name="restaurant-outline" size={26} color={colors.ink400} /><Text style={styles.emptyText}>현재 판매 중인 상품이 없어요.</Text></View>}
      </View>
    </ScrollView></ScreenEntrance>{sameStoreCart ? <CartSummarySheet actionLabel="바로 결제하기" compact onAction={() => router.push('/cart/checkout')} origin={origin}/> : null}
  </SafeAreaView>;
}

function originLabel(origin: StoreOrigin) {
  if (origin === '/stores') return '목록';
  if (origin === '/favorites') return '찜';
  if (origin === '/orders') return '주문내역';
  if (origin === '/my') return '마이';
  return '홈';
}

function DishTile({ dish, store, origin }: { dish: Dish; store: Store; origin: StoreOrigin }) {
  const discount = dish.price > 0 ? Math.round((1 - dish.discountPrice / dish.price) * 100) : 0;
  return <Pressable accessibilityRole="button" onPress={() => router.push({ pathname: '/dishes/[dishId]', params: { dishId: String(dish.dishId), storeId: String(store.storeId), storeName: store.storeName, category: store.category, origin } })} style={({ pressed }) => [styles.dish, pressed && styles.pressed]}><View style={styles.dishMedia}><Image source={getDishImageSource(dish, store.category)} style={styles.dishImage}/><View style={styles.stockBadge}><Text style={styles.stockText}>{dish.quantity > 0 ? `${dish.quantity}개 남음` : '품절'}</Text></View></View><Text numberOfLines={1} style={styles.dishName}>{dish.dishName}</Text><Text numberOfLines={2} style={styles.description}>{dish.description || '오늘 준비된 마감 할인 상품이에요.'}</Text><View style={styles.priceRow}>{discount > 0 ? <Text style={styles.discount}>{discount}%</Text> : null}<Text style={styles.price}>{dish.discountPrice.toLocaleString()}원</Text></View>{dish.price > dish.discountPrice ? <Text style={styles.originalPrice}>{dish.price.toLocaleString()}원</Text> : null}</Pressable>;
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.white }, state: { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.canvas }, stateClose: { position: 'absolute', right: 14, top: 8, zIndex: 2, width: 44, height: 44, alignItems: 'center', justifyContent: 'center', borderRadius: 22, backgroundColor: colors.white, borderWidth: 1, borderColor: colors.line }, content: { paddingBottom: 32, backgroundColor: colors.white }, contentWithCart: { paddingBottom: 188 },
  hero: { position: 'relative', width: '100%', alignSelf: 'center', overflow: 'hidden', backgroundColor: colors.canvas }, heroShade: { ...StyleSheet.absoluteFillObject, backgroundColor: 'rgba(0,0,0,0.06)' }, heroButton: { position: 'absolute', left: 14, width: 44, height: 44, alignItems: 'center', justifyContent: 'center', borderRadius: 22, backgroundColor: 'rgba(255,255,255,0.94)' }, closeButton: { position: 'absolute', right: 14, width: 44, height: 44, alignItems: 'center', justifyContent: 'center', borderRadius: 22, backgroundColor: 'rgba(255,255,255,0.94)' }, pickupBadge: { position: 'absolute', left: 18, bottom: 16, paddingHorizontal: 11, paddingVertical: 7, flexDirection: 'row', alignItems: 'center', gap: 6, borderRadius: 7, backgroundColor: 'rgba(23,26,24,0.82)' }, pickupDot: { width: 6, height: 6, borderRadius: 3, backgroundColor: colors.green300 }, pickupText: { color: colors.white, fontFamily: fonts.body, fontSize: 12, fontWeight: '800' },
  body: { width: '100%', alignSelf: 'center', paddingHorizontal: 20, backgroundColor: colors.white }, storeHead: { paddingVertical: 20, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 12 }, storeProfile: { width: 58, height: 58, borderRadius: 15, backgroundColor: colors.canvas, borderWidth: 2, borderColor: colors.white }, storeCopy: { flex: 1 }, name: { color: colors.ink900, fontFamily: fonts.body, fontSize: 27, lineHeight: 34, fontWeight: '900', letterSpacing: -1 }, facts: { marginTop: 9, flexDirection: 'row', flexWrap: 'wrap', alignItems: 'center', gap: 7 }, category: { color: colors.green700, fontFamily: fonts.body, fontSize: 12, fontWeight: '800' }, fact: { color: colors.ink700, fontFamily: fonts.body, fontSize: 12, fontWeight: '600' }, factDot: { width: 3, height: 3, borderRadius: 2, backgroundColor: colors.ink400 }, favorite: { width: 44, height: 44, alignItems: 'center', justifyContent: 'center', borderRadius: 22, backgroundColor: colors.white, borderWidth: 1, borderColor: colors.line }, pressed: { opacity: 0.7, transform: [{ scale: 0.985 }] },
  notice: { padding: 14, flexDirection: 'row', alignItems: 'flex-start', gap: 11, borderRadius: 10, backgroundColor: colors.apricot50, borderWidth: 1, borderColor: colors.apricot300 }, noticeIcon: { width: 32, height: 32, alignItems: 'center', justifyContent: 'center', borderRadius: 16, backgroundColor: colors.white }, noticeCopy: { flex: 1 }, noticeTitle: { color: colors.ink900, fontFamily: fonts.body, fontSize: 13, fontWeight: '800' }, noticeBody: { marginTop: 5, color: colors.ink700, fontFamily: fonts.body, fontSize: 12, lineHeight: 18 },
  locationHead: { marginTop: 23, marginBottom: 10, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' }, sectionTitle: { color: colors.ink900, fontFamily: fonts.body, fontSize: 18, fontWeight: '900' }, route: { minHeight: 44, paddingLeft: 16, textAlignVertical: 'center', color: colors.green700, fontFamily: fonts.body, fontSize: 12, fontWeight: '800' }, map: { position: 'relative', height: 146, overflow: 'hidden', borderRadius: radius.card, borderWidth: 1, borderColor: colors.line }, mapRecenter: { position: 'absolute', right: 8, top: 8, width: 42, height: 42, alignItems: 'center', justifyContent: 'center', borderRadius: 21, backgroundColor: colors.white, borderWidth: 1, borderColor: colors.line, ...shadow.float }, mapRecenterPressed: { opacity: 0.76, transform: [{ scale: 0.94 }] }, addressBadge: { position: 'absolute', left: 9, right: 9, bottom: 8, minHeight: 32, paddingHorizontal: 9, flexDirection: 'row', alignItems: 'center', gap: 6, borderRadius: 7, backgroundColor: 'rgba(255,255,255,0.95)' }, address: { flex: 1, color: colors.ink900, fontFamily: fonts.body, fontSize: 11, fontWeight: '600' },
  menuHead: { marginTop: 25, paddingBottom: 8, flexDirection: 'row', alignItems: 'flex-end', justifyContent: 'space-between', borderBottomWidth: 1, borderBottomColor: colors.line }, activeTab: { color: colors.ink900, fontFamily: fonts.body, fontSize: 20, fontWeight: '900', letterSpacing: -0.6 }, menuDescription: { marginTop: 4, color: colors.ink500, fontFamily: fonts.body, fontSize: 11 }, menuCount: { color: colors.ink700, fontFamily: fonts.body, fontSize: 12, fontWeight: '800' }, dishGrid: { paddingTop: 14, flexDirection: 'row', flexWrap: 'wrap', gap: 12 }, dish: { minWidth: 130, maxWidth: 240, flexBasis: 130, flexGrow: 1, paddingBottom: 12 }, dishName: { marginTop: 9, color: colors.ink900, fontFamily: fonts.body, fontSize: 15, fontWeight: '800' }, description: { marginTop: 4, minHeight: 34, color: colors.ink500, fontFamily: fonts.body, fontSize: 11, lineHeight: 16 }, priceRow: { marginTop: 7, flexDirection: 'row', alignItems: 'baseline', gap: 5 }, discount: { color: colors.green700, fontFamily: fonts.body, fontSize: 15, fontWeight: '900' }, price: { color: colors.ink900, fontFamily: fonts.body, fontSize: 15, fontWeight: '900' }, originalPrice: { marginTop: 2, color: colors.ink400, fontFamily: fonts.body, fontSize: 10, textDecorationLine: 'line-through' }, dishMedia: { position: 'relative', width: '100%', aspectRatio: 1.08 }, dishImage: { width: '100%', height: '100%', borderRadius: 10, backgroundColor: colors.canvas }, stockBadge: { position: 'absolute', right: 6, bottom: 6, paddingHorizontal: 7, paddingVertical: 4, borderRadius: 5, backgroundColor: 'rgba(20,24,21,0.8)' }, stockText: { color: colors.white, fontFamily: fonts.body, fontSize: 9, fontWeight: '800' }, empty: { paddingVertical: 38, alignItems: 'center', gap: 8 }, emptyText: { color: colors.ink400, fontFamily: fonts.body },
});
