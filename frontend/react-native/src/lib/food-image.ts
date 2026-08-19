import type { ImageSourcePropType } from 'react-native';

import type { Dish, Store } from '@/types/store';

const images = {
  korean: require('../../assets/images/food/korean-meal.png'),
  bakery: require('../../assets/images/food/bakery.png'),
  pizza: require('../../assets/images/food/pizza.png'),
  chicken: require('../../assets/images/food/chicken.png'),
} as const;

const fallbackByCategory: Record<string, ImageSourcePropType> = {
  CAFE_DESSERT: images.bakery,
  PIZZA: images.pizza,
  CHICKEN: images.chicken,
  LATE_NIGHT: images.chicken,
  FAST_FOOD: images.chicken,
};

export function getStoreCoverImageSource(store: Pick<Store, 'coverImageUrl' | 'imageUrl' | 'category'>): ImageSourcePropType {
  const url = store.coverImageUrl ?? store.imageUrl;
  return url ? { uri: url } : fallbackByCategory[store.category] ?? images.korean;
}

export function getStoreProfileImageSource(store: Pick<Store, 'profileImageUrl' | 'imageUrl' | 'category'>): ImageSourcePropType {
  const url = store.profileImageUrl ?? store.imageUrl;
  return url ? { uri: url } : fallbackByCategory[store.category] ?? images.korean;
}

export const getStoreImageSource = getStoreProfileImageSource;

export function getDishImageSource(dish: Pick<Dish, 'imageUrl'>, category?: string): ImageSourcePropType {
  return dish.imageUrl ? { uri: dish.imageUrl } : fallbackByCategory[category ?? ''] ?? images.korean;
}
