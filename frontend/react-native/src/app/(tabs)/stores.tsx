import { RoundedIcon as Ionicons } from '@/components/rounded-icon';
import * as Haptics from 'expo-haptics';
import { LinearGradient } from 'expo-linear-gradient';
import { router } from 'expo-router';
import { useEffect, useMemo, useRef, useState } from 'react';
import { FlatList, Modal, Pressable, ScrollView, StyleSheet, Text, TextInput, View, type GestureResponderEvent } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { OptimizedImage as Image } from '@/components/optimized-image';

import { EmptyState } from '@/components/empty-state';
import { AppRefreshControl } from '@/components/app-refresh-control';
import { BrandLogo } from '@/components/brand-logo';
import { LoadingState } from '@/components/loading-state';
import { FLOATING_TAB_CONTENT_INSET } from '@/components/floating-tab-bar';
import { RefreshStatus } from '@/components/refresh-status';
import { ProductStoreCard } from '@/components/product-store-card';
import { colors, fonts, radius, shadow } from '@/constants/theme';
import { useNearbyStores } from '@/hooks/use-nearby-stores';
import { usePullToRefresh } from '@/hooks/use-pull-to-refresh';
import { useResponsiveLayout } from '@/hooks/use-responsive-layout';
import { getDishImageSource } from '@/lib/food-image';
import { showLoginRequired } from '@/lib/login-required';
import { getStoreCategoryVisual } from '@/lib/store-category';
import { getDishDiscountRate, isDishAvailable } from '@/lib/store-pricing';
import { useAuth } from '@/providers/auth-provider';
import { useCart } from '@/providers/cart-provider';
import { useStoreAvailability } from '@/providers/store-availability-provider';
import type { Dish, Store } from '@/types/store';

const categories = [
  ['ALL', '전체'],
  ['KOREAN', '한식'],
  ['CHICKEN', '치킨'],
  ['PIZZA', '피자'],
  ['CAFE_DESSERT', '카페·디저트'],
  ['LATE_NIGHT', '야식·주점'],
] as const;

const MIN_RADIUS_KM = 0.1;
const MAX_RADIUS_KM = 5;
const RADIUS_STEP_KM = 0.1;
const RADIUS_HAPTIC_STEPS = new Set([0, 9, 29, 49]);
const formatRadius = (value: number) => value < 1 ? `${Math.round(value * 1000)}m` : `${Number(value.toFixed(1))}km`;
type ProductEntry = { key: string; store: Store; dish?: Dish };

export default function StoresScreen() {
  const [radiusKm, setRadiusKm] = useState(3);
  const [radiusPickerOpen, setRadiusPickerOpen] = useState(false);
  const [query, setQuery] = useState('');
  const [category, setCategory] = useState<(typeof categories)[number][0]>('ALL');
  const { stores, loading, reload } = useNearbyStores(radiusKm);
  const { refreshing, onRefresh } = usePullToRefresh(reload);
  const { contentWidth, gutter, width } = useResponsiveLayout();
  const { member } = useAuth();
  const { item: cartItem } = useCart();
  const { onlyAvailable, setOnlyAvailable } = useStoreAvailability();
  const productEntries = useMemo<ProductEntry[]>(() => {
    const keyword = query.trim().toLocaleLowerCase('ko');
    return stores.flatMap<ProductEntry>((store) => {
      const categoryMatch = category === 'ALL' || store.category === category;
      if (!categoryMatch) return [];
      const dishes = store.dishes.filter((dish) => {
        const availabilityMatch = !onlyAvailable || isDishAvailable(dish);
        const keywordMatch = !keyword || [dish.dishName, store.storeName, getStoreCategoryVisual(store.category).label, store.address]
          .some((value) => value.toLocaleLowerCase('ko').includes(keyword));
        return availabilityMatch && keywordMatch;
      });
      if (dishes.length) return dishes.map((dish) => ({ key: `${store.storeId}:${dish.dishId}`, store, dish }));
      const storeKeywordMatch = !keyword || [store.storeName, getStoreCategoryVisual(store.category).label, store.address]
        .some((value) => value.toLocaleLowerCase('ko').includes(keyword));
      return !onlyAvailable && storeKeywordMatch ? [{ key: `${store.storeId}:empty`, store, dish: undefined }] : [];
    });
  }, [category, onlyAvailable, query, stores]);
  const productCount = productEntries.filter((entry) => entry.dish).length;
  const featured = productEntries.filter((entry): entry is { key: string; store: Store; dish: Dish } => Boolean(entry.dish)).slice(0, 5);
  const featureWidth = Math.min(196, Math.max(164, width * 0.46));

  const topBar = <View style={[styles.topBar, { width: contentWidth, paddingHorizontal: gutter }]}> 
      <View style={styles.search}><BrandLogo size={30}/><TextInput accessibilityLabel="주변 상품 검색" value={query} onChangeText={setQuery} placeholder="내 주변 상품·매장 검색" placeholderTextColor={colors.ink500} returnKeyType="search" style={styles.searchInput}/>{query ? <Pressable accessibilityLabel="검색어 지우기" hitSlop={10} onPress={() => setQuery('')}><Ionicons name="close-circle" size={18} color={colors.ink400}/></Pressable> : null}</View>
      <View style={styles.headerActions}><Pressable accessibilityLabel={!member ? '알림, 로그인 필요' : '알림'} onPress={() => member ? router.push('/notifications' as never) : showLoginRequired('/notifications')} style={({ pressed }) => [styles.iconTouch, pressed && styles.pressed]}><View style={styles.iconSurface}><Ionicons name="notifications-outline" size={18} color={colors.ink900}/></View></Pressable><Pressable accessibilityLabel={!member ? '장바구니, 로그인 필요' : `장바구니${cartItem ? `, 상품 ${cartItem.cartQuantity}개` : ''}`} onPress={() => member ? router.push({ pathname: '/cart', params: { origin: '/stores' } }) : showLoginRequired('/cart?origin=/stores')} style={({ pressed }) => [styles.iconTouch, pressed && styles.pressed]}><View style={styles.iconSurface}><Ionicons name="cart-outline" size={18} color={colors.ink900}/>{member && cartItem ? <View style={styles.cartBadge}><Text style={styles.cartBadgeText}>{cartItem.cartQuantity}</Text></View> : null}</View></Pressable></View>
    </View>;
  const header = <View style={[styles.header, { width: contentWidth }]}> 
    <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={[styles.categoryList, { paddingHorizontal: gutter }]}>
      {categories.map(([key, label]) => <Pressable key={key} onPress={() => setCategory(key)} style={styles.category}><Text style={[styles.categoryText, category === key && styles.categoryTextActive]}>{label}</Text>{category === key ? <View style={styles.categoryLine}/> : null}</Pressable>)}
    </ScrollView>
    <View style={styles.divider}/>
    <View style={[styles.radiusSection, { marginHorizontal: gutter }]}> 
      <View style={styles.radiusTopRow}><View><Text style={styles.radiusTitle}>내 위치 기준</Text><Text style={styles.radiusDescription}>반경과 매장 범위를 함께 설정하세요</Text></View>
      <Pressable accessibilityLabel={`검색 반경 ${formatRadius(radiusKm)}`} accessibilityHint="검색 반경 설정을 엽니다" accessibilityRole="button" onPress={() => setRadiusPickerOpen(true)} style={({ pressed }) => [styles.radiusPickerButton, pressed && styles.pressed]}><Ionicons name="options-outline" size={16} color={colors.ink900}/><Text style={styles.radiusPickerValue}>{formatRadius(radiusKm)}</Text><Ionicons name="chevron-down" size={14} color={colors.ink500}/></Pressable></View>
      <View style={styles.availabilitySegments}><Pressable accessibilityRole="button" accessibilityState={{ selected: onlyAvailable }} onPress={() => setOnlyAvailable(true)} style={[styles.availabilitySegment, onlyAvailable && styles.availabilitySegmentActive]}><Text style={[styles.availabilitySegmentText, onlyAvailable && styles.availabilitySegmentTextActive]}>픽업 가능만</Text></Pressable><Pressable accessibilityRole="button" accessibilityState={{ selected: !onlyAvailable }} onPress={() => setOnlyAvailable(false)} style={[styles.availabilitySegment, !onlyAvailable && styles.availabilitySegmentActive]}><Text style={[styles.availabilitySegmentText, !onlyAvailable && styles.availabilitySegmentTextActive]}>전체 매장</Text></Pressable></View>
    </View>
    {featured.length ? <View style={styles.featuredSection}>
      <View style={[styles.sectionHeading, { paddingHorizontal: gutter }]}><View><Text style={styles.sectionTitle}>오늘 픽업 상품</Text><Text style={styles.sectionDescription}>{formatRadius(radiusKm)} 안에서 할인 중인 상품이에요</Text></View><Text style={styles.sectionCount}>{featured.length}개</Text></View>
      <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={[styles.featuredList, { paddingHorizontal: gutter }]}>{featured.map(({ key, store, dish }) => <FeaturedProduct dish={dish} key={key} store={store} width={featureWidth}/>)}</ScrollView>
    </View> : null}
    <View style={styles.sectionDivider}/>
    <View style={[styles.listHeading, { paddingHorizontal: gutter }]}><Text style={styles.listTitle}>{query ? '상품 검색 결과' : '가까운 상품'}</Text><View style={styles.resultBadge}><Ionicons name="location" size={13} color={colors.ink700}/><Text style={styles.resultText}>{formatRadius(radiusKm)} · 상품 {productCount}개</Text></View></View>
  </View>;

  return <SafeAreaView style={styles.safe} edges={['top']}>{topBar}<LinearGradient colors={[colors.canvas, colors.canvasWarm, colors.white]} end={{ x: 0.5, y: 1 }} pointerEvents="none" start={{ x: 0.5, y: 0 }} style={styles.topFade}/><RefreshStatus visible={refreshing}/><FlatList
    style={styles.listView}
    alwaysBounceVertical
    data={productEntries}
    keyExtractor={(item) => item.key}
    ListHeaderComponent={header}
    contentContainerStyle={[styles.list, { width: contentWidth, paddingBottom: FLOATING_TAB_CONTENT_INSET }]}
    renderItem={({ item }) => <View style={{ paddingHorizontal: gutter }}><ProductStoreCard dish={item.dish} store={item.store} onPress={() => item.dish ? router.push({ pathname: '/dishes/[dishId]', params: { dishId: String(item.dish.dishId), origin: '/stores' } }) : router.push({ pathname: '/stores/[storeId]', params: { storeId: String(item.store.storeId), origin: '/stores' } })}/></View>}
    refreshControl={<AppRefreshControl refreshing={refreshing} onRefresh={onRefresh}/>}
    showsVerticalScrollIndicator={false}
    ListEmptyComponent={loading ? <LoadingState label="주변 마감 상품을 찾고 있어요"/> : <EmptyState title={query ? '검색한 상품이 없어요' : '주변 픽업 상품을 찾지 못했어요'} description={query ? '상품명이나 카테고리를 바꿔보세요.' : '전체 매장을 선택하거나 검색 반경을 넓혀보세요.'}/>}
  /><RadiusPicker visible={radiusPickerOpen} value={radiusKm} onClose={() => setRadiusPickerOpen(false)} onApply={(value) => { setRadiusKm(value); setRadiusPickerOpen(false); }}/></SafeAreaView>;
}

function RadiusPicker({ visible, value, onClose, onApply }: { visible: boolean; value: number; onClose: () => void; onApply: (value: number) => void }) {
  const [draft, setDraft] = useState(value);
  const [trackWidth, setTrackWidth] = useState(0);
  const frameRef = useRef<number | null>(null);
  const trackLeftRef = useRef(0);
  const pendingStepRef = useRef(Math.round((value - MIN_RADIUS_KM) / RADIUS_STEP_KM));
  const lastStepRef = useRef(pendingStepRef.current);
  const progress = (draft - MIN_RADIUS_KM) / (MAX_RADIUS_KM - MIN_RADIUS_KM);

  useEffect(() => {
    if (!visible) return;
    const step = Math.round((value - MIN_RADIUS_KM) / RADIUS_STEP_KM);
    pendingStepRef.current = step;
    lastStepRef.current = step;
    setDraft(value);
  }, [value, visible]);

  useEffect(() => () => {
    if (frameRef.current !== null) cancelAnimationFrame(frameRef.current);
  }, []);

  const updateStep = (step: number) => {
    const boundedStep = Math.max(0, Math.min(49, step));
    if (boundedStep === lastStepRef.current) return;

    const direction = boundedStep > lastStepRef.current ? 1 : -1;
    for (let index = lastStepRef.current + direction; direction > 0 ? index <= boundedStep : index >= boundedStep; index += direction) {
      if (RADIUS_HAPTIC_STEPS.has(index)) {
        void Haptics.selectionAsync();
        break;
      }
    }
    lastStepRef.current = boundedStep;
    pendingStepRef.current = boundedStep;

    if (frameRef.current !== null) return;
    frameRef.current = requestAnimationFrame(() => {
      frameRef.current = null;
      setDraft(Number((MIN_RADIUS_KM + pendingStepRef.current * RADIUS_STEP_KM).toFixed(1)));
    });
  };

  const setFromPosition = (event: GestureResponderEvent) => {
    if (!trackWidth) return;
    const nextProgress = Math.max(0, Math.min(1, (event.nativeEvent.pageX - trackLeftRef.current) / trackWidth));
    updateStep(Math.round(nextProgress * 49));
  };
  const startSliding = (event: GestureResponderEvent) => {
    trackLeftRef.current = event.nativeEvent.pageX - event.nativeEvent.locationX;
    setFromPosition(event);
  };
  const adjust = (direction: -1 | 1) => updateStep(lastStepRef.current + direction);

  return <Modal animationType="fade" onRequestClose={onClose} presentationStyle="overFullScreen" transparent visible={visible}>
    <View style={styles.modalRoot}>
      <Pressable accessibilityLabel="검색 반경 설정 닫기" accessibilityRole="button" onPress={onClose} style={styles.scrim}/>
      <SafeAreaView edges={['bottom']} style={[styles.sheetStage, { width: '100%', maxWidth: 560, alignSelf: 'center', overflow: 'hidden', borderTopLeftRadius: radius.sheet, borderTopRightRadius: radius.sheet, backgroundColor: colors.white }]}>
        <View pointerEvents="none" style={[styles.safeAreaFill, { top: radius.sheet }]}/>
        <View accessibilityViewIsModal style={[styles.sheet, { paddingBottom: 18 }]}>
          <View style={styles.sheetHandle}/>
          <View style={styles.sheetHeader}><View><Text style={styles.sheetTitle}>검색 반경 설정</Text><Text style={styles.sheetDescription}>내 위치에서 최대 5km까지 찾아볼 수 있어요.</Text></View><Pressable accessibilityLabel="검색 반경 설정 닫기" accessibilityRole="button" onPress={onClose} style={({ pressed }) => [styles.sheetClose, pressed && styles.pressed]}><Ionicons name="close" size={23} color={colors.ink900}/></Pressable></View>
          <View accessibilityLiveRegion="polite" style={styles.radiusPreview}><Text style={styles.previewLabel}>선택한 반경</Text><Text style={styles.previewValue}>{formatRadius(draft)}</Text></View>
          <View
            accessible
            accessibilityActions={[{ name: 'decrement', label: '100미터 줄이기' }, { name: 'increment', label: '100미터 늘리기' }]}
            accessibilityLabel="검색 반경"
            accessibilityRole="adjustable"
            accessibilityValue={{ min: 100, max: 5000, now: Math.round(draft * 1000), text: formatRadius(draft) }}
            onAccessibilityAction={(event) => event.nativeEvent.actionName === 'increment' ? adjust(1) : event.nativeEvent.actionName === 'decrement' ? adjust(-1) : undefined}
            onLayout={(event) => setTrackWidth(event.nativeEvent.layout.width)}
            onMoveShouldSetResponder={() => true}
            onResponderGrant={startSliding}
            onResponderMove={setFromPosition}
            onStartShouldSetResponder={() => true}
            style={styles.sliderTouch}>
            <View style={styles.sliderTrack}>
              <View style={[styles.sliderFill, { width: `${progress * 100}%` }]}/>
              <View pointerEvents="none" style={[styles.sliderTick, styles.sliderTickMin, draft >= 0.1 && styles.sliderTickActive]}>{draft >= 0.1 ? <Ionicons name="checkmark" size={9} color={colors.white}/> : null}</View>
              <View pointerEvents="none" style={[styles.sliderTick, styles.sliderTickOne, draft >= 1 && styles.sliderTickActive]}>{draft >= 1 ? <Ionicons name="checkmark" size={9} color={colors.white}/> : null}</View>
              <View pointerEvents="none" style={[styles.sliderTick, styles.sliderTickThree, draft >= 3 && styles.sliderTickActive]}>{draft >= 3 ? <Ionicons name="checkmark" size={9} color={colors.white}/> : null}</View>
              <View pointerEvents="none" style={[styles.sliderTick, styles.sliderTickMax, draft >= 5 && styles.sliderTickActive]}>{draft >= 5 ? <Ionicons name="checkmark" size={9} color={colors.white}/> : null}</View>
              <View pointerEvents="none" style={[styles.sliderThumb, { left: `${progress * 100}%`, width: 22, height: 22, marginLeft: -11, marginTop: -11, borderRadius: 11, borderWidth: 5 }]}/>
            </View>
          </View>
          <View style={styles.sliderLabels}><Text style={[styles.sliderLabel, styles.sliderLabelMin]}>100m</Text><Text style={[styles.sliderLabel, styles.sliderLabelOne]}>1km</Text><Text style={[styles.sliderLabel, styles.sliderLabelThree]}>3km</Text><Text style={[styles.sliderLabel, styles.sliderLabelMax]}>5km</Text></View>
          <Text style={styles.sliderHint}>막대를 좌우로 움직여 100m 단위로 조절하세요.</Text>
          <View style={styles.sheetActions}><Pressable accessibilityRole="button" onPress={onClose} style={({ pressed }) => [styles.cancelButton, pressed && styles.pressed]}><Text style={styles.cancelText}>취소</Text></Pressable><Pressable accessibilityRole="button" onPress={() => onApply(draft)} style={({ pressed }) => [styles.applyButton, pressed && styles.pressed]}><Text style={styles.applyText}>{formatRadius(draft)}로 적용</Text></Pressable></View>
        </View>
      </SafeAreaView>
    </View>
  </Modal>;
}

function FeaturedProduct({ dish, store, width }: { dish: Dish; store: Store; width: number }) {
  const discountRate = getDishDiscountRate(dish);
  return <Pressable accessibilityHint="상품 상세를 엽니다" accessibilityLabel={`${dish.dishName}, ${dish.discountPrice.toLocaleString()}원, ${store.storeName}`} accessibilityRole="button" onPress={() => router.push({ pathname: '/dishes/[dishId]', params: { dishId: String(dish.dishId), origin: '/stores' } })} style={({ pressed }) => [styles.featureCard, { width }, pressed && styles.pressed]}><View><Image accessibilityLabel={`${dish.dishName} 상품 이미지`} source={getDishImageSource(dish, store.category)} style={[styles.featureImage, { width }]}/>{discountRate > 0 ? <View style={styles.featureDiscount}><Text style={styles.featureDiscountText}>{discountRate}% 할인</Text></View> : null}</View><View style={styles.featureCopy}><Text numberOfLines={2} style={styles.featureName}>{dish.dishName}</Text><Text numberOfLines={1} style={styles.featureMeta}>{store.storeName}</Text><View style={styles.featurePriceRow}><Text style={styles.featurePrice}>{dish.discountPrice.toLocaleString()}원</Text>{dish.price > dish.discountPrice ? <Text style={styles.featureOriginalPrice}>{dish.price.toLocaleString()}원</Text> : null}</View></View></Pressable>;
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.white }, listView: { flex: 1 }, list: { alignSelf: 'center', flexGrow: 1 }, header: { alignSelf: 'center', backgroundColor: colors.white },
  topBar: { alignSelf: 'center', paddingTop: 10, paddingBottom: 8, flexDirection: 'row', alignItems: 'center', gap: 5, backgroundColor: colors.canvas }, topFade: { height: 16 }, search: { minWidth: 0, height: 44, flex: 1, paddingLeft: 7, paddingRight: 12, flexDirection: 'row', alignItems: 'center', gap: 8, borderRadius: 22, backgroundColor: colors.white, ...shadow.control }, searchInput: { minWidth: 0, flex: 1, height: 44, paddingVertical: 0, color: colors.ink900, fontFamily: fonts.body, fontSize: 14, fontWeight: '600' }, headerActions: { flexDirection: 'row', gap: 5, backgroundColor: 'transparent' }, iconTouch: { width: 42, height: 42, alignItems: 'center', justifyContent: 'center', backgroundColor: 'transparent' }, iconSurface: { width: 38, height: 38, alignItems: 'center', justifyContent: 'center', borderRadius: 19, backgroundColor: colors.white, borderWidth: StyleSheet.hairlineWidth, borderColor: 'rgba(20,28,22,.08)', ...shadow.control }, cartBadge: { position: 'absolute', right: 0, top: -3, minWidth: 16, height: 16, paddingHorizontal: 3, alignItems: 'center', justifyContent: 'center', borderRadius: 8, backgroundColor: colors.green500, borderWidth: 1.5, borderColor: colors.white }, cartBadgeText: { color: colors.ink900, fontFamily: fonts.body, fontSize: 10, fontWeight: '700' },
  categoryList: { gap: 25 }, category: { minHeight: 48, alignItems: 'center', justifyContent: 'center' }, categoryText: { color: colors.ink400, fontFamily: fonts.body, fontSize: 15, fontWeight: '700' }, categoryTextActive: { color: colors.ink900, fontWeight: '900' }, categoryLine: { position: 'absolute', left: 0, right: 0, bottom: 0, height: 3, borderRadius: 2, backgroundColor: colors.ink900 }, divider: { height: 1, backgroundColor: colors.line },
  radiusSection: { marginTop: 12, padding: 10, gap: 10, borderRadius: radius.input, backgroundColor: colors.canvas }, radiusTopRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 12 }, radiusTitle: { color: colors.ink900, fontFamily: fonts.body, fontSize: 13, fontWeight: '800' }, radiusDescription: { marginTop: 2, color: colors.ink500, fontFamily: fonts.body, fontSize: 10 }, radiusPickerButton: { minWidth: 94, minHeight: 44, paddingHorizontal: 12, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 6, borderRadius: radius.control, backgroundColor: colors.white, borderWidth: 1, borderColor: colors.lineStrong }, radiusPickerValue: { color: colors.ink900, fontFamily: fonts.body, fontSize: 13, fontWeight: '900' }, availabilitySegments: { minHeight: 42, padding: 3, flexDirection: 'row', borderRadius: radius.control, backgroundColor: colors.white, borderWidth: 1, borderColor: colors.line }, availabilitySegment: { flex: 1, alignItems: 'center', justifyContent: 'center', borderRadius: radius.control - 3 }, availabilitySegmentActive: { backgroundColor: colors.green500 }, availabilitySegmentText: { color: colors.ink700, fontFamily: fonts.body, fontSize: 12, fontWeight: '800' }, availabilitySegmentTextActive: { color: colors.white },
  featuredSection: { paddingTop: 18, paddingBottom: 18 }, sectionHeading: { marginBottom: 10, flexDirection: 'row', justifyContent: 'space-between', alignItems: 'flex-end' }, sectionTitle: { color: colors.ink900, fontFamily: fonts.body, fontSize: 20, fontWeight: '900', letterSpacing: -0.6 }, sectionDescription: { marginTop: 3, color: colors.ink500, fontFamily: fonts.body, fontSize: 12 }, sectionCount: { color: colors.ink700, fontFamily: fonts.body, fontSize: 12, fontWeight: '700' }, featuredList: { gap: 10 }, featureCard: { overflow: 'hidden', borderRadius: radius.card, backgroundColor: colors.white, borderWidth: 1, borderColor: colors.line, ...shadow.card }, featureImage: { aspectRatio: 4 / 3, backgroundColor: colors.canvas }, featureDiscount: { position: 'absolute', left: 8, bottom: 8, paddingHorizontal: 8, paddingVertical: 5, borderRadius: 6, backgroundColor: colors.ink900 }, featureDiscountText: { color: colors.white, fontFamily: fonts.body, fontSize: 10, fontWeight: '900' }, featureCopy: { minHeight: 91, padding: 10 }, featureName: { color: colors.ink900, fontFamily: fonts.body, fontSize: 15, lineHeight: 20, fontWeight: '800' }, featureMeta: { marginTop: 4, color: colors.ink500, fontFamily: fonts.body, fontSize: 10, fontWeight: '700' }, featurePriceRow: { marginTop: 6, flexDirection: 'row', alignItems: 'baseline', gap: 6 }, featurePrice: { color: colors.ink900, fontFamily: fonts.body, fontSize: 15, fontWeight: '900', fontVariant: ['tabular-nums'] }, featureOriginalPrice: { color: colors.ink400, fontFamily: fonts.body, fontSize: 9, textDecorationLine: 'line-through', fontVariant: ['tabular-nums'] },
  sectionDivider: { height: 8, backgroundColor: colors.canvas }, listHeading: { paddingTop: 14, paddingBottom: 8, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' }, listTitle: { color: colors.ink900, fontFamily: fonts.body, fontSize: 17, fontWeight: '900' }, resultBadge: { flexDirection: 'row', alignItems: 'center', gap: 4 }, resultText: { color: colors.ink700, fontFamily: fonts.body, fontSize: 12, fontWeight: '700' }, pressed: { opacity: 0.72, transform: [{ scale: 0.99 }] },
  modalRoot: { flex: 1, justifyContent: 'flex-end' }, scrim: { ...StyleSheet.absoluteFillObject, backgroundColor: 'rgba(15, 20, 17, 0.46)' }, sheetStage: { width: '100%', alignItems: 'center' }, safeAreaFill: { ...StyleSheet.absoluteFillObject, backgroundColor: colors.white }, sheet: { width: '100%', maxWidth: 560, paddingHorizontal: 20, paddingTop: 8, paddingBottom: 6, borderTopLeftRadius: radius.sheet, borderTopRightRadius: radius.sheet, backgroundColor: colors.white, ...shadow.float }, sheetHandle: { alignSelf: 'center', width: 36, height: 4, borderRadius: 2, backgroundColor: colors.lineStrong }, sheetHeader: { minHeight: 78, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 12 }, sheetTitle: { color: colors.ink900, fontFamily: fonts.body, fontSize: 20, fontWeight: '900', letterSpacing: -0.5 }, sheetDescription: { marginTop: 4, color: colors.ink500, fontFamily: fonts.body, fontSize: 11 }, sheetClose: { width: 44, height: 44, alignItems: 'center', justifyContent: 'center', borderRadius: 22, backgroundColor: colors.canvas }, radiusPreview: { minHeight: 72, paddingHorizontal: 15, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', borderRadius: radius.input, backgroundColor: colors.canvas }, previewLabel: { color: colors.ink700, fontFamily: fonts.body, fontSize: 13, fontWeight: '700' }, previewValue: { color: colors.ink900, fontFamily: fonts.body, fontSize: 25, fontWeight: '900', letterSpacing: -0.8 }, sliderTouch: { height: 60, marginTop: 18, justifyContent: 'center' }, sliderTrack: { height: 3, borderRadius: 2, backgroundColor: colors.line }, sliderFill: { position: 'absolute', left: 0, height: 3, borderRadius: 2, backgroundColor: colors.green500 }, sliderTick: { position: 'absolute', top: -5, zIndex: 2, width: 13, height: 13, marginLeft: -6.5, alignItems: 'center', justifyContent: 'center', borderRadius: 7, backgroundColor: colors.line, borderWidth: 1.5, borderColor: colors.white }, sliderTickActive: { backgroundColor: colors.green500 }, sliderTickMin: { left: 0 }, sliderTickOne: { left: '18.37%' }, sliderTickThree: { left: '59.18%' }, sliderTickMax: { left: '100%' }, sliderThumb: { position: 'absolute', top: '50%', zIndex: 3, width: 28, height: 28, marginLeft: -14, marginTop: -14, borderRadius: 14, backgroundColor: colors.white, borderWidth: 7, borderColor: colors.ink900, ...shadow.card }, sliderLabels: { position: 'relative', height: 18, marginTop: -4 }, sliderLabel: { position: 'absolute', color: colors.ink500, fontFamily: fonts.body, fontSize: 10, fontWeight: '700' }, sliderLabelMin: { left: 0 }, sliderLabelOne: { left: '18.37%', width: 36, marginLeft: -18, textAlign: 'center' }, sliderLabelThree: { left: '59.18%', width: 36, marginLeft: -18, textAlign: 'center' }, sliderLabelMax: { right: 0 }, sliderHint: { marginTop: 12, color: colors.ink500, fontFamily: fonts.body, fontSize: 11, textAlign: 'center' }, sheetActions: { marginTop: 22, flexDirection: 'row', gap: 8 }, cancelButton: { minHeight: 52, flex: 0.7, alignItems: 'center', justifyContent: 'center', borderRadius: radius.input, borderWidth: 1, borderColor: colors.lineStrong, backgroundColor: colors.white }, cancelText: { color: colors.ink900, fontFamily: fonts.body, fontSize: 14, fontWeight: '800' }, applyButton: { minHeight: 52, flex: 1.3, alignItems: 'center', justifyContent: 'center', borderRadius: radius.input, backgroundColor: colors.green500 }, applyText: { color: colors.white, fontFamily: fonts.body, fontSize: 14, fontWeight: '900' },
});
