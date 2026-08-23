import { RoundedIcon as Ionicons } from '@/components/rounded-icon';
import * as Haptics from 'expo-haptics';
import { router } from 'expo-router';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Animated, Image, Keyboard, PanResponder, Platform, Pressable, ScrollView, StyleSheet, Text, TextInput, useWindowDimensions, View } from 'react-native';
import Reanimated, { Easing as ReanimatedEasing, useAnimatedStyle, useSharedValue, withTiming } from 'react-native-reanimated';
import { SafeAreaView, useSafeAreaInsets } from 'react-native-safe-area-context';

import { BrandLogo } from '@/components/brand-logo';
import { AppRefreshControl } from '@/components/app-refresh-control';
import { MapCanvas } from '@/components/map-canvas';
import type { MapCamera as CameraState, MapCameraCommand, MapCoordinate as Coordinate, MapCameraEventSource } from '@/components/map-canvas.types';
import { ScreenEntrance } from '@/components/motion';
import { RefreshStatus } from '@/components/refresh-status';
import { colors, fonts, motion, radius, shadow } from '@/constants/theme';
import { useNearbyStores } from '@/hooks/use-nearby-stores';
import { usePullToRefresh } from '@/hooks/use-pull-to-refresh';
import { useResponsiveLayout } from '@/hooks/use-responsive-layout';
import { useReducedMotion } from '@/hooks/use-reduced-motion';
import { getStoreCategoryVisual, STORE_CATEGORY_KEYS } from '@/lib/store-category';
import { getStoreProfileImageSource } from '@/lib/food-image';
import { showLoginRequired } from '@/lib/login-required';
import { formatCheapestDishOffer, getCheapestDish } from '@/lib/store-pricing';
import { useAuth } from '@/providers/auth-provider';
import { useCart } from '@/providers/cart-provider';
import type { Store } from '@/types/store';

const homeCategories = STORE_CATEGORY_KEYS;
type HomeCategory = (typeof homeCategories)[number];

const distanceKm = (a: Coordinate, b: Coordinate) => {
  const toRad = (value: number) => value * Math.PI / 180;
  const dLat = toRad(b.latitude - a.latitude);
  const dLon = toRad(b.longitude - a.longitude);
  const x = Math.sin(dLat / 2) ** 2 + Math.cos(toRad(a.latitude)) * Math.cos(toRad(b.latitude)) * Math.sin(dLon / 2) ** 2;
  return 6371 * 2 * Math.atan2(Math.sqrt(x), Math.sqrt(1 - x));
};

export default function HomeScreen() {
  const { stores, loading, error, reload, location, locationResolved, heading } = useNearbyStores(3);
  const { member } = useAuth();
  const { item: cartItem } = useCart();
  const { contentWidth, gutter, isCompact } = useResponsiveLayout();
  const reducedMotion = useReducedMotion();
  const { top, bottom } = useSafeAreaInsets();
  const floatingBarGap = Math.max(20, Math.min(28, bottom - 6));
  const floatingBarClearance = floatingBarGap + 64 + 10;
  const [selected, setSelected] = useState<Store | null>(null);
  const [cameraCommand, setCameraCommand] = useState<MapCameraCommand>();
  const [mapBearing, setMapBearing] = useState(0);
  const [mapCamera, setMapCamera] = useState<CameraState>({ ...location, zoom: 14.5, bearing: 0 });
  const [pendingCenter, setPendingCenter] = useState<CameraState | null>(null);
  const [areaRefreshing, setAreaRefreshing] = useState(false);
  const [query, setQuery] = useState('');
  const [searchOpen, setSearchOpen] = useState(false);
  const [searchFocused, setSearchFocused] = useState(false);
  const [underTen, setUnderTen] = useState(false);
  const [selectedCategory, setSelectedCategory] = useState<HomeCategory>();
  const [mapControlsHidden, setMapControlsHidden] = useState(false);
  const loadedCenter = useRef<Coordinate>(location);
  const programmaticCameraUntil = useRef(0);
  const centeredOnResolvedLocation = useRef(false);
  const sheetVisibleHeight = useRef(new Animated.Value(130)).current;
  const mapControlsOpacity = useRef(new Animated.Value(1)).current;
  const searchExpansion = useSharedValue(0);

  useEffect(() => { loadedCenter.current = location; }, [location]);
  useEffect(() => {
    Animated.timing(mapControlsOpacity, { toValue: mapControlsHidden ? 0 : 1, duration: 140, useNativeDriver: true }).start();
  }, [mapControlsHidden, mapControlsOpacity]);
  useEffect(() => {
    searchExpansion.value = withTiming(searchFocused ? 1 : 0, {
      duration: reducedMotion ? 0 : motion.base,
      easing: ReanimatedEasing.bezier(0.22, 1, 0.36, 1),
    });
  }, [reducedMotion, searchExpansion, searchFocused]);
  const headerActionsAnimatedStyle = useAnimatedStyle(() => ({
    width: 91 * (1 - searchExpansion.value),
    opacity: 1 - searchExpansion.value,
    transform: [{ translateX: 12 * searchExpansion.value }],
  }));
  const searchResultsAnimatedStyle = useAnimatedStyle(() => ({
    opacity: searchExpansion.value,
    transform: [{ translateY: -4 * (1 - searchExpansion.value) }],
  }));

  const filteredStores = useMemo(() => stores.filter((store) => {
    const categoryMatches = !selectedCategory || store.category === selectedCategory;
    const priceMatches = !underTen || store.dishes.some((dish) => dish.discountPrice <= 10_000);
    return categoryMatches && priceMatches;
  }), [selectedCategory, stores, underTen]);
  const markerStores = useMemo(() => {
    if (mapCamera.zoom < 13.5) return [];
    const radiusByZoom = Math.min(3, Math.max(0.18, 8 / (2 ** (mapCamera.zoom - 12))));
    const limit = mapCamera.zoom < 14.5 ? 10 : mapCamera.zoom < 16 ? 18 : 30;
    return filteredStores
      .filter((store) => distanceKm(mapCamera, store) <= radiusByZoom)
      .sort((left, right) => distanceKm(mapCamera, left) - distanceKm(mapCamera, right))
      .slice(0, limit);
  }, [filteredStores, mapCamera]);
  const searchResults = useMemo(() => {
    const keyword = query.trim().toLocaleLowerCase('ko');
    if (!keyword) return [];
    return stores.filter((store) => [store.storeName, getStoreCategoryVisual(store.category).label, store.address]
      .some((value) => value.toLocaleLowerCase('ko').includes(keyword))).slice(0, 5);
  }, [query, stores]);

  const issueCameraCommand = useCallback((command: MapCameraCommand) => {
    programmaticCameraUntil.current = Date.now() + 650;
    setMapCamera((current) => {
        if (command.type === 'zoomIn' || command.type === 'zoomOut') {
          const currentZoom = Platform.OS === 'web' ? Math.round(current.zoom) : current.zoom;
          return { ...current, zoom: Math.max(10, Math.min(20, currentZoom + (command.type === 'zoomIn' ? 1 : -1))) };
        }
        if (command.type === 'location') return { ...location, zoom: 15, bearing: current.bearing };
        if (command.type === 'focus') return { latitude: command.latitude, longitude: command.longitude, zoom: command.zoom ?? 16, bearing: current.bearing };
        if (command.type === 'heading') return { ...current, bearing: command.bearing };
        return current;
      });
    setCameraCommand(command);
    if (Platform.OS === 'web') window.dispatchEvent(new CustomEvent('lastdish-map-command', { detail: command }));
  }, [location]);

  useEffect(() => {
    if (!locationResolved || centeredOnResolvedLocation.current) return;
    centeredOnResolvedLocation.current = true;
    loadedCenter.current = location;
    setPendingCenter(null);
    issueCameraCommand({ id: Date.now(), type: 'location' });
  }, [issueCameraCommand, location, locationResolved]);

  const focusStore = (store: Store) => {
    setSelected(store);
    setQuery(store.storeName);
    setSearchOpen(false);
    setSearchFocused(false);
    Keyboard.dismiss();
    issueCameraCommand({ id: Date.now(), type: 'focus', latitude: store.latitude, longitude: store.longitude, zoom: 16 });
  };

  const handleCameraIdle = (camera: CameraState, source?: MapCameraEventSource) => {
    setMapCamera(camera);
    setMapBearing(camera.bearing);
    if (source === 'gesture') {
      setPendingCenter(distanceKm(loadedCenter.current, camera) >= 0.01 ? camera : null);
      return;
    }
    if (Date.now() < programmaticCameraUntil.current) {
      return;
    }
    setPendingCenter(distanceKm(loadedCenter.current, camera) >= 0.01 ? camera : null);
  };

  const refreshArea = async () => {
    if (!pendingCenter || areaRefreshing) return;
    const next = pendingCenter;
    setSelected(null);
    setAreaRefreshing(true);
    try {
      await reload(next);
      loadedCenter.current = next;
      setPendingCenter(null);
    } finally {
      setAreaRefreshing(false);
    }
  };
  const refreshSheetStores = useCallback(() => reload(loadedCenter.current, true), [reload]);
  const { refreshing: sheetRefreshing, onRefresh: refreshSheet } = usePullToRefresh(refreshSheetStores);

  return (
    <SafeAreaView style={styles.safe} edges={[]}>
      <ScreenEntrance style={styles.mapWrap}>
        <MapCanvas
          stores={markerStores}
          center={location}
          userLocation={location}
          userHeading={heading}
          cameraCommand={cameraCommand}
          selectedStoreId={selected?.storeId}
          onCameraIdle={handleCameraIdle}
          onSelect={setSelected}
        />

        <View style={[styles.header, { top: top + 8, width: contentWidth - gutter * 2 }]}> 
          <View style={styles.searchGroup}>
            <View style={styles.search}>
              <BrandLogo size={30} />
              <TextInput
                accessibilityLabel="지도에서 매장 검색"
                autoCorrect={false}
                onChangeText={(value) => { setQuery(value); setSearchOpen(Boolean(value.trim())); }}
                onFocus={() => { setSearchFocused(true); setSearchOpen(Boolean(query.trim())); }}
                placeholder="남부터미널역 · 매장 검색"
                placeholderTextColor={colors.ink500}
                returnKeyType="search"
                style={[styles.searchInput, isCompact && styles.searchInputCompact]}
                value={query}
              />
              {searchFocused ? <Pressable accessibilityLabel="검색 닫기" hitSlop={10} onPress={() => { setQuery(''); setSearchOpen(false); setSearchFocused(false); Keyboard.dismiss(); }}><Ionicons name="close" size={20} color={colors.ink700}/></Pressable> : query ? <Pressable accessibilityLabel="검색어 지우기" hitSlop={8} onPress={() => { setQuery(''); setSearchOpen(false); }}><Ionicons name="close-circle" size={18} color={colors.ink400}/></Pressable> : null}
            </View>
            {searchOpen ? <Reanimated.View style={[styles.searchResults, searchResultsAnimatedStyle]}>
              {searchResults.length ? searchResults.map((store) => {
                const category = getStoreCategoryVisual(store.category);
                return <Pressable key={store.storeId} onPress={() => focusStore(store)} style={({ pressed }) => [styles.searchResult, pressed && styles.pressed]}>
                  <View style={[styles.resultIcon, { backgroundColor: category.color }]}><Ionicons name={category.icon} size={15} color={colors.white}/></View>
                  <View style={styles.resultCopy}><Text numberOfLines={1} style={styles.resultName}>{store.storeName}</Text><Text numberOfLines={1} style={styles.resultMeta}>{category.label} · {store.address}</Text></View>
                  <Ionicons name="locate-outline" size={18} color={colors.green700}/>
                </Pressable>;
              }) : <Text style={styles.noResult}>현재 지도 범위에서 일치하는 매장이 없어요.</Text>}
            </Reanimated.View> : null}
          </View>
          <Reanimated.View pointerEvents={searchFocused ? 'none' : 'auto'} style={[styles.headerActions, headerActionsAnimatedStyle]}>
            <Pressable accessibilityLabel={!member ? '알림, 로그인 필요' : '알림'} hitSlop={2} onPress={() => member ? router.push('/notifications' as never) : showLoginRequired('/notifications')} style={({ pressed }) => [styles.iconTouch, pressed && styles.pressed]}><View style={styles.iconSurface}><Ionicons name="notifications-outline" size={18} color={colors.ink900}/></View></Pressable>
            <Pressable accessibilityLabel={!member ? '장바구니, 로그인 필요' : `장바구니${cartItem ? `, 상품 ${cartItem.cartQuantity}개` : ''}`} accessibilityHint={!member ? '로그인 안내가 열립니다' : undefined} hitSlop={2} style={({ pressed }) => [styles.iconTouch, styles.lastIconTouch, pressed && styles.pressed]} onPress={() => member ? router.push({ pathname: '/cart', params: { origin: '/' } }) : showLoginRequired('/cart?origin=/')}><View style={styles.iconSurface}><Ionicons name="cart-outline" size={18} color={colors.ink900}/>{member && cartItem && <View style={styles.cartBadge}><Text style={styles.cartBadgeText}>{cartItem.cartQuantity}</Text></View>}</View></Pressable>
          </Reanimated.View>
        </View>

        {!searchFocused && !searchOpen ? <ScrollView horizontal showsHorizontalScrollIndicator={false} style={[styles.chipScroller, { top: top + 57 }]} contentContainerStyle={[styles.chips, { paddingHorizontal: gutter }]}> 
          <View style={[styles.chip, styles.chipActive]}><Ionicons name="time" size={13} color={colors.green700}/><Text style={[styles.chipText, styles.chipActiveText]}>지금 픽업</Text></View>
          <Pressable onPress={() => { setUnderTen((value) => !value); setSelected(null); }} style={({ pressed }) => [styles.chip, underTen && styles.chipActive, pressed && styles.pressed]}><Text style={[styles.chipText, underTen && styles.chipActiveText]}>1만원 이하</Text></Pressable>
          {homeCategories.map((key) => { const visual = getStoreCategoryVisual(key); const active = selectedCategory === key; return <Pressable accessibilityRole="button" accessibilityState={{ selected: active }} key={key} onPress={() => { setSelectedCategory((current) => current === key ? undefined : key); setSelected(null); void Haptics.selectionAsync(); }} style={({ pressed }) => [styles.chip, active && styles.chipActive, pressed && styles.pressed]}><Ionicons name={visual.icon} size={13} color={active ? colors.green700 : colors.ink700}/><Text style={[styles.chipText, active && styles.chipActiveText]}>{visual.label}</Text></Pressable>; })}
        </ScrollView> : null}

        {pendingCenter && !searchOpen ? <Pressable accessibilityRole="button" accessibilityState={{ busy: areaRefreshing, disabled: areaRefreshing }} disabled={areaRefreshing} onPress={() => void refreshArea()} style={({ pressed }) => [styles.areaRefresh, { top: top + 111 }, pressed && !areaRefreshing && styles.pressed]}>
          <Ionicons name="refresh" size={16} color={colors.green700}/><Text style={styles.areaRefreshText}>{areaRefreshing ? '이 지역 매장을 찾는 중' : '이 지역 매장 검색'}</Text>
        </Pressable> : null}

        <Animated.View pointerEvents={mapControlsHidden ? 'none' : 'auto'} style={[styles.mapActionStack, { bottom: floatingBarClearance, opacity: mapControlsOpacity, transform: [{ translateY: Animated.multiply(sheetVisibleHeight, -1) }] }]}>
          <Pressable accessibilityLabel="나침반, 현재 바라보는 방향으로 지도 정렬" style={({ pressed }) => [styles.compass, pressed && styles.controlPressed]} onPress={() => issueCameraCommand({ id: Date.now(), type: 'heading', bearing: heading })}>
            <View style={[styles.compassRose, { transform: [{ rotate: `${-mapBearing}deg` }] }]}><Text style={styles.compassNorth}>N</Text><Ionicons name="navigate" size={16} color={colors.ink900}/></View>
          </Pressable>
          <View style={styles.mapControls}><Pressable accessibilityLabel={`지도 확대, 현재 줌 ${mapCamera.zoom.toFixed(1)}`} style={({ pressed }) => [styles.control, pressed && styles.controlPressed]} onPress={() => issueCameraCommand({ id: Date.now(), type: 'zoomIn' })}><Ionicons name="add" size={21} color={colors.ink900} /></Pressable><View style={styles.controlLine}/><Pressable accessibilityLabel={`지도 축소, 현재 줌 ${mapCamera.zoom.toFixed(1)}`} style={({ pressed }) => [styles.control, pressed && styles.controlPressed]} onPress={() => issueCameraCommand({ id: Date.now(), type: 'zoomOut' })}><Ionicons name="remove" size={21} color={colors.ink900} /></Pressable></View>
          <Pressable accessibilityLabel="내 위치로 이동" style={({ pressed }) => [styles.recenter, pressed && styles.controlPressed]} onPress={() => { setSelected(null); setPendingCenter(null); loadedCenter.current = location; issueCameraCommand({ id: Date.now(), type: 'location' }); }}><Ionicons name="locate" size={19} color={colors.green500} /></Pressable>
        </Animated.View>

        {(loading || error) && !pendingCenter && <View style={[styles.notice, { top: top + 111, width: Math.min(contentWidth - gutter * 2, 360) }]}> 
          {loading ? <BrandLogo size={28}/> : <Ionicons name="cloud-offline-outline" size={18} color={colors.ink700} />}
          <Text style={styles.noticeText}>{loading ? '이 지역 매장을 찾는 중' : '매장 정보를 불러오지 못했어요'}</Text>
          {error && <Text onPress={() => void reload()} style={styles.retry}>재시도</Text>}
        </View>}

        <HomeStoreSheet bottomOffset={floatingBarClearance} stores={filteredStores} location={location} selected={selected} refreshing={sheetRefreshing} visibleHeight={sheetVisibleHeight} onControlsHiddenChange={setMapControlsHidden} onClearSelection={() => setSelected(null)} onRefresh={refreshSheet} onSelect={focusStore}/>
      </ScreenEntrance>
    </SafeAreaView>
  );
}

type SheetLevel = 0 | 1 | 2;

function HomeStoreSheet({ bottomOffset, stores, location, selected, refreshing, visibleHeight, onControlsHiddenChange, onClearSelection, onRefresh, onSelect }: { bottomOffset: number; stores: Store[]; location: Coordinate; selected: Store | null; refreshing: boolean; visibleHeight: Animated.Value; onControlsHiddenChange: (hidden: boolean) => void; onClearSelection: () => void; onRefresh: () => void; onSelect: (store: Store) => void }) {
  const { height } = useWindowDimensions();
  const expandedHeight = Math.min(650, Math.max(400, (height - bottomOffset) * 0.72));
  const snapOffsets = useMemo(() => [expandedHeight - 34, expandedHeight - 130, 0], [expandedHeight]);
  const [level, setLevel] = useState<SheetLevel>(1);
  const translateY = useRef(new Animated.Value(snapOffsets[1])).current;
  const dragStart = useRef(snapOffsets[1]);
  const dragCurrent = useRef(snapOffsets[1]);
  const controlsHiddenRef = useRef(false);

  const updateControlsVisibility = useCallback((hidden: boolean) => {
    if (controlsHiddenRef.current === hidden) return;
    controlsHiddenRef.current = hidden;
    onControlsHiddenChange(hidden);
  }, [onControlsHiddenChange]);

  const snapTo = useCallback((next: SheetLevel) => {
    setLevel(next);
    updateControlsVisibility(next === 2);
    void Haptics.selectionAsync();
    Animated.parallel([
      Animated.spring(translateY, { toValue: snapOffsets[next], damping: 24, stiffness: 260, mass: 0.85, useNativeDriver: true }),
      Animated.spring(visibleHeight, { toValue: expandedHeight - snapOffsets[next], damping: 24, stiffness: 260, mass: 0.85, useNativeDriver: true }),
    ]).start();
  }, [expandedHeight, snapOffsets, translateY, updateControlsVisibility, visibleHeight]);

  const settleAt = useCallback((projected: number) => {
    const bounded = Math.max(0, Math.min(snapOffsets[0], projected));
    const revealedRatio = (expandedHeight - bounded) / expandedHeight;
    if (revealedRatio > 0.4) {
      snapTo(2);
      return;
    }
    if (expandedHeight - bounded < 40) {
      snapTo(0);
      return;
    }
    setLevel(1);
    updateControlsVisibility(false);
    Animated.parallel([
      Animated.spring(translateY, { toValue: bounded, damping: 28, stiffness: 300, mass: 0.8, useNativeDriver: true }),
      Animated.spring(visibleHeight, { toValue: expandedHeight - bounded, damping: 28, stiffness: 300, mass: 0.8, useNativeDriver: true }),
    ]).start();
  }, [expandedHeight, snapOffsets, snapTo, translateY, updateControlsVisibility, visibleHeight]);

  useEffect(() => {
    if (selected) snapTo(1);
  }, [selected, snapTo]);

  const panResponder = useMemo(() => PanResponder.create({
    onMoveShouldSetPanResponder: (_, gesture) => Math.abs(gesture.dy) > 5 && Math.abs(gesture.dy) > Math.abs(gesture.dx),
    onPanResponderGrant: () => {
      visibleHeight.stopAnimation();
      translateY.stopAnimation((value) => { dragStart.current = value; dragCurrent.current = value; });
    },
    onPanResponderMove: (_, gesture) => {
      const next = Math.max(0, Math.min(snapOffsets[0], dragStart.current + gesture.dy));
      dragCurrent.current = next;
      translateY.setValue(next);
      visibleHeight.setValue(expandedHeight - next);
      updateControlsVisibility((expandedHeight - next) / expandedHeight > 0.4);
    },
    onPanResponderRelease: (_, gesture) => {
      const projected = Math.max(0, Math.min(snapOffsets[0], dragStart.current + gesture.dy + gesture.vy * 70));
      settleAt(projected);
    },
    onPanResponderTerminate: () => settleAt(dragCurrent.current),
  }), [expandedHeight, settleAt, snapOffsets, translateY, updateControlsVisibility, visibleHeight]);

  return <Animated.View style={[styles.storeSheet, { bottom: bottomOffset, height: expandedHeight, transform: [{ translateY }] }]}>
    <View accessibilityLabel="주변 매장 목록 높이 조절" accessibilityRole="adjustable" style={styles.storeSheetHandleArea} {...panResponder.panHandlers}><View style={styles.storeSheetHandle}/></View>
    {selected ? <Pressable accessibilityHint="매장 상세 미리보기를 펼칩니다" onPress={() => snapTo(2)} style={({ pressed }) => [styles.selectedStore, pressed && styles.pressed]}>
      <Image accessibilityLabel={`${selected.storeName} 프로필 이미지`} source={getStoreProfileImageSource(selected)} style={styles.selectedStoreImage}/>
      <View style={styles.selectedStoreCopy}><Text numberOfLines={1} style={styles.storeRowName}>{selected.storeName}</Text><Text style={styles.storeRowMeta}>{getStoreCategoryVisual(selected.category).label} · {selected.closeTime?.slice(0, 5) ?? '오늘'} 마감</Text><Text numberOfLines={1} style={styles.selectedStoreAddress}>{getCheapestDish(selected) ? `${getCheapestDish(selected)!.dishName} · ${formatCheapestDishOffer(selected)}` : selected.address}</Text></View>
      <Pressable accessibilityLabel="매장 선택 닫기" hitSlop={8} onPress={(event) => { event.stopPropagation(); onClearSelection(); }} style={styles.selectedStoreClose}><Ionicons name="close" size={18} color={colors.ink700}/></Pressable>
    </Pressable> : <Pressable accessibilityRole="button" onPress={() => snapTo(level === 2 ? 1 : 2)} style={styles.storeSheetHeading}>
      <View><Text style={styles.storeSheetTitle}>주변 매장</Text><Text style={styles.storeSheetMeta}>내 위치 가까운 순 · {stores.length}곳</Text></View>
      <View style={styles.storeSheetExpand}><Ionicons name={level === 2 ? 'chevron-down' : 'chevron-up'} size={18} color={colors.ink900}/></View>
    </Pressable>}
    {!selected ? <RefreshStatus visible={refreshing}/> : null}
    <Animated.ScrollView
      alwaysBounceVertical
      contentContainerStyle={styles.storeSheetList}
      refreshControl={!selected ? <AppRefreshControl refreshing={refreshing} onRefresh={onRefresh}/> : undefined}
      scrollEnabled={level === 2}
      showsVerticalScrollIndicator={false}>
      {selected ? <SelectedStoreDetail store={selected} distance={distanceKm(location, selected)}/> : stores.map((store) => <HomeStoreRow key={store.storeId} store={store} distance={distanceKm(location, store)} onPress={() => onSelect(store)}/>)}
      {!selected && !stores.length ? <View style={styles.storeSheetEmpty}><Text style={styles.storeSheetEmptyTitle}>이 지역에 표시할 매장이 없어요</Text><Text style={styles.storeSheetEmptyBody}>지도를 이동하고 이 지역 매장 검색을 눌러보세요.</Text></View> : null}
    </Animated.ScrollView>
  </Animated.View>;
}

function SelectedStoreDetail({ store, distance }: { store: Store; distance: number }) {
  return <View style={styles.selectedDetail}>
    <View style={styles.selectedDetailTop}><View><Text style={styles.selectedDetailEyebrow}>매장 미리보기</Text><Text style={styles.selectedDetailTitle}>오늘 픽업 가능한 상품</Text></View><Text style={styles.selectedDetailDistance}>{distance < 1 ? `${Math.round(distance * 1000)}m` : `${distance.toFixed(1)}km`}</Text></View>
    {store.dishes.slice(0, 3).map((dish) => <View key={dish.dishId} style={styles.previewDish}><View style={styles.previewDishIcon}><Ionicons name="restaurant-outline" size={17} color={colors.ink700}/></View><View style={styles.previewDishCopy}><Text numberOfLines={1} style={styles.previewDishName}>{dish.dishName}</Text><Text style={styles.previewDishMeta}>남은 수량 {dish.quantity}개</Text></View><Text style={styles.previewDishPrice}>{dish.discountPrice.toLocaleString()}원</Text></View>)}
    {!store.dishes.length ? <Text style={styles.noPreviewDish}>등록된 마감 할인 상품을 매장 상세에서 확인해보세요.</Text> : null}
    <Pressable accessibilityRole="button" onPress={() => router.push({ pathname: '/stores/[storeId]', params: { storeId: String(store.storeId), origin: '/' } })} style={({ pressed }) => [styles.detailButton, pressed && styles.pressed]}><Text style={styles.detailButtonText}>매장 상세 보기</Text><Ionicons name="arrow-forward" size={17} color={colors.white}/></Pressable>
  </View>;
}

function HomeStoreRow({ store, distance, onPress }: { store: Store; distance: number; onPress: () => void }) {
  const category = getStoreCategoryVisual(store.category);
  return <Pressable accessibilityRole="button" onPress={onPress} style={({ pressed }) => [styles.storeRow, pressed && styles.pressed]}>
    <Image accessibilityLabel={`${store.storeName} 프로필 이미지`} source={getStoreProfileImageSource(store)} style={styles.storeRowImage}/>
    <View style={styles.storeRowCopy}><Text numberOfLines={1} style={styles.storeRowName}>{store.storeName}</Text><Text style={styles.storeRowMeta}>{category.label} · {distance < 1 ? `${Math.round(distance * 1000)}m` : `${distance.toFixed(1)}km`} · {store.closeTime?.slice(0, 5) ?? '오늘'} 마감</Text><Text style={styles.storeRowPrice}>{formatCheapestDishOffer(store)}</Text></View>
    <Ionicons name="chevron-forward" size={17} color={colors.ink400}/>
  </Pressable>;
}

const homeControlShadow = {
  shadowColor: colors.ink900,
  shadowOpacity: 0.16,
  shadowRadius: 4,
  shadowOffset: { width: 0, height: 3 },
  elevation: 4,
} as const;

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.canvas },
  mapWrap: { flex: 1, overflow: 'hidden' },
  header: { position: 'absolute', alignSelf: 'center', flexDirection: 'row', zIndex: 20, backgroundColor: 'transparent' },
  searchGroup: { flex: 1, minWidth: 0, backgroundColor: 'transparent' },
  headerActions: { flexDirection: 'row', gap: 0, paddingLeft: 3, overflow: 'visible', backgroundColor: 'transparent' },
  search: { height: 44, flexDirection: 'row', alignItems: 'center', gap: 8, paddingLeft: 7, paddingRight: 12, backgroundColor: colors.white, borderRadius: 22, ...homeControlShadow },
  searchInput: { flex: 1, height: 44, paddingVertical: 0, color: colors.ink900, fontSize: 15, fontWeight: '600', fontFamily: fonts.body },
  searchInputCompact: { fontSize: 14 },
  searchResults: { position: 'absolute', top: 50, left: 0, right: 0, overflow: 'hidden', borderRadius: radius.card, backgroundColor: colors.white, ...shadow.float },
  searchResult: { minHeight: 56, paddingHorizontal: 10, flexDirection: 'row', alignItems: 'center', gap: 9, borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: colors.line },
  resultIcon: { width: 30, height: 30, borderRadius: 9, alignItems: 'center', justifyContent: 'center' },
  resultCopy: { flex: 1 },
  resultName: { color: colors.ink900, fontFamily: fonts.body, fontSize: 14, fontWeight: '800' },
  resultMeta: { marginTop: 2, color: colors.ink500, fontFamily: fonts.body, fontSize: 11 },
  noResult: { padding: 15, color: colors.ink500, fontFamily: fonts.body, fontSize: 12, lineHeight: 18 },
  iconTouch: { width: 44, height: 44, alignItems: 'center', justifyContent: 'center' },
  lastIconTouch: { alignItems: 'flex-end' },
  iconSurface: { width: 38, height: 38, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.white, borderRadius: 19, borderWidth: StyleSheet.hairlineWidth, borderColor: 'rgba(20,28,22,.08)', ...homeControlShadow },
  cartBadge: { position: 'absolute', right: 0, top: -3, minWidth: 16, height: 16, paddingHorizontal: 3, alignItems: 'center', justifyContent: 'center', borderRadius: 8, backgroundColor: colors.green500, borderWidth: 1.5, borderColor: colors.white },
  cartBadgeText: { color: colors.ink900, fontFamily: fonts.body, fontSize: 10, fontWeight: '700' },
  chipScroller: { position: 'absolute', left: 0, right: 0, zIndex: 10, backgroundColor: 'transparent' },
  chips: { flexDirection: 'row', gap: 6, paddingTop: 7, paddingBottom: 10, paddingRight: 30, backgroundColor: 'transparent' },
  chip: { minHeight: 34, paddingHorizontal: 12, flexDirection: 'row', gap: 5, alignItems: 'center', justifyContent: 'center', borderRadius: radius.pill, backgroundColor: colors.white, borderWidth: 1, borderColor: 'rgba(20,28,22,.14)', ...homeControlShadow },
  chipActive: { borderColor: colors.green500 },
  chipText: { color: colors.ink700, fontSize: 13, fontWeight: '700', fontFamily: fonts.body },
  chipActiveText: { color: colors.green700 },
  areaRefresh: { position: 'absolute', alignSelf: 'center', minHeight: 38, paddingHorizontal: 14, flexDirection: 'row', alignItems: 'center', gap: 6, borderRadius: radius.pill, backgroundColor: colors.white, borderWidth: 1, borderColor: colors.green300, ...shadow.float },
  areaRefreshText: { color: colors.green700, fontFamily: fonts.body, fontSize: 13, fontWeight: '800' },
  mapActionStack: { position: 'absolute', right: 14, alignItems: 'center', gap: 9, zIndex: 12 },
  mapControls: { width: 42, overflow: 'hidden', borderRadius: radius.input, backgroundColor: colors.white, borderWidth: 1, borderColor: colors.line, ...shadow.float },
  compass: { width: 42, height: 42, alignItems: 'center', justifyContent: 'center', borderRadius: 21, backgroundColor: colors.white, borderWidth: 1, borderColor: colors.line, ...shadow.float },
  compassRose: { width: 32, height: 32, alignItems: 'center', justifyContent: 'center' },
  compassNorth: { position: 'absolute', top: -1, color: '#E2473E', fontFamily: fonts.body, fontSize: 9, fontWeight: '900' },
  control: { width: 42, height: 40, alignItems: 'center', justifyContent: 'center' },
  controlLine: { height: 1, backgroundColor: colors.line },
  controlPressed: { backgroundColor: colors.green50 },
  recenter: { width: 42, height: 42, alignItems: 'center', justifyContent: 'center', borderRadius: 21, backgroundColor: colors.white, borderWidth: 1, borderColor: colors.line, ...shadow.float },
  notice: { position: 'absolute', alignSelf: 'center', minHeight: 46, paddingHorizontal: 10, flexDirection: 'row', alignItems: 'center', gap: 7, borderRadius: radius.input, backgroundColor: colors.white, ...shadow.card },
  noticeText: { flex: 1, color: colors.ink700, fontSize: 13, fontWeight: '700', fontFamily: fonts.body },
  retry: { color: colors.green700, fontWeight: '800', fontFamily: fonts.body },
  storeSheet: { position: 'absolute', left: 0, right: 0, zIndex: 15, overflow: 'hidden', borderTopLeftRadius: 22, borderTopRightRadius: 22, backgroundColor: colors.white, borderTopWidth: 1, borderTopColor: colors.lineStrong, ...shadow.float },
  storeSheetHandleArea: { height: 22, paddingTop: 8 },
  storeSheetHandle: { alignSelf: 'center', width: 38, height: 4, borderRadius: 2, backgroundColor: colors.lineStrong },
  storeSheetHeading: { minHeight: 76, paddingHorizontal: 17, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: colors.line },
  storeSheetTitle: { color: colors.ink900, fontFamily: fonts.body, fontSize: 18, fontWeight: '900', letterSpacing: -0.45 },
  storeSheetMeta: { marginTop: 3, color: colors.ink500, fontFamily: fonts.body, fontSize: 10, fontWeight: '600' },
  storeSheetExpand: { width: 34, height: 34, alignItems: 'center', justifyContent: 'center', borderRadius: 17, backgroundColor: colors.canvas },
  selectedStore: { minHeight: 98, paddingHorizontal: 13, paddingBottom: 8, flexDirection: 'row', alignItems: 'center', gap: 11 },
  selectedStoreImage: { width: 68, height: 68, borderRadius: 11, backgroundColor: colors.canvas },
  selectedStoreCopy: { flex: 1, minWidth: 0 },
  selectedStoreAddress: { marginTop: 4, color: colors.ink400, fontFamily: fonts.body, fontSize: 10 },
  selectedStoreClose: { width: 32, height: 32, alignItems: 'center', justifyContent: 'center', borderRadius: 16, backgroundColor: colors.canvas },
  selectedDetail: { paddingHorizontal: 13, paddingBottom: 28 },
  selectedDetailTop: { paddingVertical: 17, flexDirection: 'row', alignItems: 'flex-end', justifyContent: 'space-between', borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: colors.line },
  selectedDetailEyebrow: { color: colors.green700, fontFamily: fonts.body, fontSize: 10, fontWeight: '800' },
  selectedDetailTitle: { marginTop: 4, color: colors.ink900, fontFamily: fonts.body, fontSize: 18, fontWeight: '900', letterSpacing: -0.45 },
  selectedDetailDistance: { color: colors.ink500, fontFamily: fonts.body, fontSize: 11, fontWeight: '700' },
  previewDish: { minHeight: 70, flexDirection: 'row', alignItems: 'center', gap: 10, borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: colors.line },
  previewDishIcon: { width: 38, height: 38, alignItems: 'center', justifyContent: 'center', borderRadius: 10, backgroundColor: colors.canvas },
  previewDishCopy: { flex: 1, minWidth: 0 },
  previewDishName: { color: colors.ink900, fontFamily: fonts.body, fontSize: 14, fontWeight: '800' },
  previewDishMeta: { marginTop: 4, color: colors.ink500, fontFamily: fonts.body, fontSize: 10 },
  previewDishPrice: { color: colors.ink900, fontFamily: fonts.body, fontSize: 13, fontWeight: '900' },
  noPreviewDish: { paddingVertical: 28, color: colors.ink500, fontFamily: fonts.body, fontSize: 12, lineHeight: 19, textAlign: 'center' },
  detailButton: { minHeight: 50, marginTop: 16, paddingHorizontal: 16, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 7, borderRadius: radius.input, backgroundColor: colors.ink900 },
  detailButtonText: { color: colors.white, fontFamily: fonts.body, fontSize: 14, fontWeight: '900' },
  storeSheetList: { paddingHorizontal: 13, paddingTop: 3, paddingBottom: 28 },
  storeRow: { minHeight: 92, flexDirection: 'row', alignItems: 'center', gap: 11, borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: colors.line },
  storeRowImage: { width: 68, height: 68, borderRadius: 10, backgroundColor: colors.canvas },
  storeRowFallback: { alignItems: 'center', justifyContent: 'center' },
  storeRowCopy: { flex: 1, minWidth: 0 },
  storeRowName: { color: colors.ink900, fontFamily: fonts.body, fontSize: 15, fontWeight: '900', letterSpacing: -0.3 },
  storeRowMeta: { marginTop: 5, color: colors.ink500, fontFamily: fonts.body, fontSize: 11, fontWeight: '600' },
  storeRowPrice: { marginTop: 5, color: colors.ink900, fontFamily: fonts.body, fontSize: 12, fontWeight: '800' },
  storeSheetEmpty: { paddingVertical: 45, alignItems: 'center' },
  storeSheetEmptyTitle: { color: colors.ink900, fontFamily: fonts.body, fontSize: 15, fontWeight: '900' },
  storeSheetEmptyBody: { marginTop: 6, color: colors.ink500, fontFamily: fonts.body, fontSize: 11 },
  pressed: { opacity: .72, transform: [{ scale: .99 }] },
});
