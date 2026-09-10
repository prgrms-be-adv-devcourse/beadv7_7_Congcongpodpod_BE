import { RoundedIcon as Ionicons } from '@/components/rounded-icon';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { OptimizedImage as Image } from '@/components/optimized-image';

import { colors, fonts, radius, shadow, typography } from '@/constants/theme';
import { getStoreImageSource } from '@/lib/food-image';
import { getStoreCategoryVisual } from '@/lib/store-category';
import { formatCheapestDishOffer, formatStoreOperatingHours } from '@/lib/store-pricing';
import type { Store } from '@/types/store';

type Props = { store: Store; onPress?: () => void; favorite?: boolean; compact?: boolean };

export function StoreCard({ store, onPress, favorite, compact = false }: Props) {
  return (
    <Pressable accessibilityHint="매장 상세를 엽니다" accessibilityRole="button" onPress={onPress} style={({ pressed }) => [styles.row, compact && styles.compactRow, pressed && styles.pressed]}>
      <View style={[styles.media, compact && styles.compactMedia]}>
        <Image accessibilityLabel={`${store.storeName} 프로필 이미지`} source={getStoreImageSource(store)} style={[styles.image, compact && styles.compactImage]} />
        <View style={styles.openBadge}><Text style={styles.openText}>픽업 가능</Text></View>
      </View>
      <View style={styles.copy}>
        <View style={styles.titleRow}>
          <Text numberOfLines={1} style={styles.title}>{store.storeName}</Text>
          {favorite ? <Ionicons name="heart" size={18} color={colors.green700} /> : null}
        </View>
        <Text style={styles.time}>{getStoreCategoryVisual(store.category).label} · {formatStoreOperatingHours(store)}</Text>
        <Text numberOfLines={1} style={styles.address}>{store.address || '주소 정보 없음'}</Text>
        <Text numberOfLines={1} style={styles.price}>{formatCheapestDishOffer(store)}</Text>
      </View>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  row: { minHeight: 138, flexDirection: 'row', alignItems: 'center', gap: 14, padding: 10, borderWidth: 1, borderColor: colors.line, borderRadius: radius.card, backgroundColor: colors.white, ...shadow.card },
  compactRow: { minHeight: 92, gap: 11, paddingHorizontal: 0, paddingVertical: 11, borderWidth: 0, borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: colors.line, borderRadius: 0, shadowOpacity: 0, elevation: 0 },
  pressed: { opacity: 0.72, transform: [{ scale: 0.992 }] },
  media: { width: 118, height: 118 },
  compactMedia: { width: 68, height: 68 },
  image: { width: 118, height: 118, borderRadius: 9, backgroundColor: colors.canvas },
  compactImage: { width: 68, height: 68, borderRadius: 10 },
  openBadge: { position: 'absolute', right: 5, bottom: 5, paddingHorizontal: 6, paddingVertical: 3, borderRadius: 5, backgroundColor: 'rgba(25,34,28,0.76)' },
  openText: { color: colors.white, fontFamily: fonts.body, fontSize: 9, fontWeight: '700' },
  copy: { flex: 1, minWidth: 0 },
  titleRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 6 },
  title: { ...typography.cardTitle, flex: 1, color: colors.ink900, fontFamily: fonts.body },
  time: { marginTop: 5, color: colors.green700, fontFamily: fonts.body, fontSize: 12, fontWeight: '700' },
  address: { ...typography.meta, marginTop: 4, color: colors.ink700, fontFamily: fonts.body },
  price: { marginTop: 5, color: colors.ink900, fontFamily: fonts.body, fontSize: 13, fontWeight: '800' },
});
