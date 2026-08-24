import type { ComponentProps } from 'react';
import { RoundedIcon as Ionicons } from '@/components/rounded-icon';

type IconName = ComponentProps<typeof Ionicons>['name'];

type CategoryVisual = {
  label: string;
  icon: IconName;
  markerImage: number;
  selectedMarkerImage: number;
  color: string;
};

const markerImages = {
  restaurantCoral: require('../../assets/images/map-icons/restaurant-coral.png'),
  restaurantRed: require('../../assets/images/map-icons/restaurant-red.png'),
  restaurantBrown: require('../../assets/images/map-icons/restaurant-brown.png'),
  restaurantOchre: require('../../assets/images/map-icons/restaurant-ochre.png'),
  fastFoodAmber: require('../../assets/images/map-icons/fast-food-amber.png'),
  fastFoodOrange: require('../../assets/images/map-icons/fast-food-orange.png'),
  fishBlue: require('../../assets/images/map-icons/fish-blue.png'),
  pizzaCoral: require('../../assets/images/map-icons/pizza-coral.png'),
  cafeAmber: require('../../assets/images/map-icons/cafe-amber.png'),
  flameRed: require('../../assets/images/map-icons/flame-red.png'),
  winePurple: require('../../assets/images/map-icons/wine-purple.png'),
  nutritionGreen: require('../../assets/images/map-icons/nutrition-green.png'),
  restaurantSelected: require('../../assets/images/map-icons/restaurant-selected.png'),
  fastFoodSelected: require('../../assets/images/map-icons/fast-food-selected.png'),
  fishSelected: require('../../assets/images/map-icons/fish-selected.png'),
  pizzaSelected: require('../../assets/images/map-icons/pizza-selected.png'),
  cafeSelected: require('../../assets/images/map-icons/cafe-selected.png'),
  flameSelected: require('../../assets/images/map-icons/flame-selected.png'),
  wineSelected: require('../../assets/images/map-icons/wine-selected.png'),
  nutritionSelected: require('../../assets/images/map-icons/nutrition-selected.png'),
} as const;

export const STORE_CATEGORY_KEYS = [
  'CHICKEN', 'CHINESE', 'BUNSIK', 'KOREAN', 'SOUP_STEW', 'CUTLET_SUSHI', 'PIZZA',
  'CAFE_DESSERT', 'FAST_FOOD', 'JOKBAL_BOSSAM', 'MEAT', 'LATE_NIGHT', 'WESTERN',
  'ASIAN', 'LUNCH_BOX',
] as const;

export type StoreCategoryKey = (typeof STORE_CATEGORY_KEYS)[number];

const categories: Record<StoreCategoryKey, CategoryVisual> = {
  CHICKEN: { label: '치킨', icon: 'restaurant', markerImage: markerImages.restaurantCoral, selectedMarkerImage: markerImages.restaurantSelected, color: '#F4774B' },
  CHINESE: { label: '중식', icon: 'restaurant', markerImage: markerImages.restaurantCoral, selectedMarkerImage: markerImages.restaurantSelected, color: '#E56A44' },
  BUNSIK: { label: '분식', icon: 'fast-food', markerImage: markerImages.fastFoodAmber, selectedMarkerImage: markerImages.fastFoodSelected, color: '#F19B27' },
  KOREAN: { label: '한식', icon: 'restaurant', markerImage: markerImages.restaurantCoral, selectedMarkerImage: markerImages.restaurantSelected, color: '#F4774B' },
  SOUP_STEW: { label: '찜·탕', icon: 'restaurant', markerImage: markerImages.restaurantRed, selectedMarkerImage: markerImages.restaurantSelected, color: '#D95F45' },
  CUTLET_SUSHI: { label: '돈까스·회', icon: 'fish', markerImage: markerImages.fishBlue, selectedMarkerImage: markerImages.fishSelected, color: '#438ACB' },
  PIZZA: { label: '피자', icon: 'pizza', markerImage: markerImages.pizzaCoral, selectedMarkerImage: markerImages.pizzaSelected, color: '#E75C4E' },
  CAFE_DESSERT: { label: '카페·디저트', icon: 'cafe', markerImage: markerImages.cafeAmber, selectedMarkerImage: markerImages.cafeSelected, color: '#ECA527' },
  FAST_FOOD: { label: '패스트푸드', icon: 'fast-food', markerImage: markerImages.fastFoodOrange, selectedMarkerImage: markerImages.fastFoodSelected, color: '#E8842E' },
  JOKBAL_BOSSAM: { label: '족발·보쌈', icon: 'restaurant', markerImage: markerImages.restaurantBrown, selectedMarkerImage: markerImages.restaurantSelected, color: '#B96A46' },
  MEAT: { label: '고기', icon: 'flame', markerImage: markerImages.flameRed, selectedMarkerImage: markerImages.flameSelected, color: '#D85C45' },
  LATE_NIGHT: { label: '야식', icon: 'wine', markerImage: markerImages.winePurple, selectedMarkerImage: markerImages.wineSelected, color: '#6B66C7' },
  WESTERN: { label: '양식', icon: 'restaurant', markerImage: markerImages.restaurantCoral, selectedMarkerImage: markerImages.restaurantSelected, color: '#CF6C4F' },
  ASIAN: { label: '아시안', icon: 'restaurant', markerImage: markerImages.restaurantOchre, selectedMarkerImage: markerImages.restaurantSelected, color: '#B87D32' },
  LUNCH_BOX: { label: '도시락', icon: 'nutrition', markerImage: markerImages.nutritionGreen, selectedMarkerImage: markerImages.nutritionSelected, color: '#4C9A68' },
};

const fallback: CategoryVisual = { label: '음식점', icon: 'restaurant', markerImage: markerImages.restaurantCoral, selectedMarkerImage: markerImages.restaurantSelected, color: '#F4774B' };

const categoryAliases = Object.fromEntries(
  STORE_CATEGORY_KEYS.flatMap((key) => [[key, key], [categories[key].label, key]]),
) as Record<string, StoreCategoryKey>;

export function getStoreCategoryVisual(category?: string): CategoryVisual {
  if (!category) return fallback;
  const normalized = category.trim().toUpperCase().replace(/[\s-]+/g, '_');
  const key = categoryAliases[category.trim()] ?? categoryAliases[normalized];
  return key ? categories[key] : { ...fallback, label: category };
}
