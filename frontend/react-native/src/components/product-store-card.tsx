import { RoundedIcon as Ionicons } from '@/components/rounded-icon';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { OptimizedImage as Image } from '@/components/optimized-image';

import { colors, fonts, radius, typography } from '@/constants/theme';
import { getDishImageSource, getStoreImageSource } from '@/lib/food-image';
import { getDishDiscountRate } from '@/lib/store-pricing';
import type { Dish, Store } from '@/types/store';

type Props = {
  dish?: Dish;
  distanceKm?: number;
  store: Store;
  onPress: () => void;
};

const formatDistance = (value?: number) => value === undefined
  ? undefined
  : value < 1 ? `${Math.round(value * 1000)}m` : `${value.toFixed(1)}km`;

const formatPickupTime = (dish?: Dish) => {
  const start = dish?.pickupStartTime?.slice(0, 5);
  const end = dish?.pickupEndTime?.slice(0, 5);
  if (start && end) return `${start}–${end} 픽업`;
  if (end) return `${end}까지 픽업`;
  return '오늘 픽업';
};

export function ProductStoreCard({ dish, distanceKm, store, onPress }: Props) {
  const discountRate = getDishDiscountRate(dish);
  const distance = formatDistance(distanceKm);
  const accessibilityLabel = dish
    ? `${dish.dishName}, ${dish.discountPrice.toLocaleString()}원, ${store.storeName}, ${distance ? `직선거리 ${distance}, ` : ''}${formatPickupTime(dish)}`
    : `${store.storeName}, ${distance ? `직선거리 ${distance}, ` : ''}등록된 픽업 상품 없음`;

  return (
    <Pressable
      accessibilityHint={dish ? '상품 상세를 엽니다' : '매장 상세를 엽니다'}
      accessibilityLabel={accessibilityLabel}
      accessibilityRole="button"
      onPress={onPress}
      style={({ pressed }) => [styles.row, pressed && styles.pressed]}>
      <View style={styles.media}>
        <Image
          accessibilityLabel={dish ? `${dish.dishName} 상품 이미지` : `${store.storeName} 매장 이미지`}
          source={dish ? getDishImageSource(dish, store.category) : getStoreImageSource(store)}
          style={styles.image}
        />
        {discountRate > 0 ? <View style={styles.discountBadge}><Text style={styles.discountBadgeText}>{discountRate}%</Text></View> : null}
      </View>
      <View style={styles.copy}>
        {dish ? <>
          <Text numberOfLines={2} style={styles.productName}>{dish.dishName}</Text>
          <View style={styles.priceRow}>
            <Text style={styles.discountPrice}>{dish.discountPrice.toLocaleString()}원</Text>
            {dish.price > dish.discountPrice ? <Text style={styles.originalPrice}>{dish.price.toLocaleString()}원</Text> : null}
          </View>
          <View style={styles.pickupRow}>
            <Ionicons name="time-outline" size={14} color={colors.green700}/>
            <Text style={styles.pickupText}>{formatPickupTime(dish)}</Text>
          </View>
          <View style={styles.storeRow}>
            <Text numberOfLines={1} style={styles.storeName}>{store.storeName}</Text>
            {distance ? <><View style={styles.storeDot}/><Ionicons name="navigate-outline" size={11} color={colors.ink500}/><Text style={styles.distance}>{distance}</Text></> : null}
            <Ionicons name="chevron-forward" size={14} color={colors.ink400}/>
          </View>
        </> : <>
          <Text numberOfLines={1} style={styles.productName}>{store.storeName}</Text>
          <Text style={styles.emptyTitle}>현재 등록된 픽업 상품이 없어요</Text>
          <Text numberOfLines={1} style={styles.emptyMeta}>{[distance, store.address || '매장 정보를 확인해보세요'].filter(Boolean).join(' · ')}</Text>
        </>}
      </View>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  row: { minHeight: 132, paddingVertical: 14, flexDirection: 'row', alignItems: 'center', gap: 14, borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: colors.line, backgroundColor: colors.white },
  pressed: { opacity: 0.68, transform: [{ scale: 0.994 }] },
  media: { width: 104, height: 104 },
  image: { width: 104, height: 104, borderRadius: radius.control, backgroundColor: colors.canvas },
  discountBadge: { position: 'absolute', left: 7, bottom: 7, minHeight: 25, paddingHorizontal: 8, alignItems: 'center', justifyContent: 'center', borderRadius: 6, backgroundColor: colors.ink900 },
  discountBadgeText: { color: colors.white, fontFamily: fonts.body, fontSize: 11, fontWeight: '900', fontVariant: ['tabular-nums'] },
  copy: { flex: 1, minWidth: 0, alignSelf: 'stretch', justifyContent: 'center' },
  productName: { ...typography.cardTitle, color: colors.ink900, fontFamily: fonts.body, fontSize: 17, lineHeight: 22 },
  priceRow: { marginTop: 7, flexDirection: 'row', alignItems: 'baseline', gap: 7 },
  discountPrice: { color: colors.ink900, fontFamily: fonts.body, fontSize: 18, lineHeight: 23, fontWeight: '900', fontVariant: ['tabular-nums'] },
  originalPrice: { color: colors.ink400, fontFamily: fonts.body, fontSize: 11, textDecorationLine: 'line-through', fontVariant: ['tabular-nums'] },
  pickupRow: { minHeight: 24, marginTop: 5, flexDirection: 'row', alignItems: 'center', gap: 4 },
  pickupText: { color: colors.green700, fontFamily: fonts.body, fontSize: 11, fontWeight: '800' },
  storeRow: { marginTop: 5, flexDirection: 'row', alignItems: 'center', gap: 2 },
  storeName: { maxWidth: '70%', color: colors.ink500, fontFamily: fonts.body, fontSize: 11, fontWeight: '700' },
  storeDot: { width: 3, height: 3, marginHorizontal: 2, borderRadius: 2, backgroundColor: colors.ink400 },
  distance: { color: colors.ink500, fontFamily: fonts.body, fontSize: 10, fontWeight: '800', fontVariant: ['tabular-nums'] },
  emptyTitle: { marginTop: 7, color: colors.ink700, fontFamily: fonts.body, fontSize: 12, fontWeight: '700' },
  emptyMeta: { marginTop: 5, color: colors.ink500, fontFamily: fonts.body, fontSize: 10 },
});
