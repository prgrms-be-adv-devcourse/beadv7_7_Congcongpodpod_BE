import type { Dish, Store } from '@/types/store';

export function hasAvailableDish(store: Pick<Store, 'dishes' | 'hasAvailableDish'>) {
  if (typeof store.hasAvailableDish === 'boolean') return store.hasAvailableDish;
  return store.dishes.some(isDishAvailable);
}

export function isDishAvailable(dish: Pick<Dish, 'quantity' | 'status'>) {
  return dish.quantity > 0 && !['SOLD_OUT', 'CLOSED'].includes(dish.status ?? '');
}

export function getCheapestDish(store: Pick<Store, 'dishes'>): Dish | undefined {
  return store.dishes.reduce<Dish | undefined>((cheapest, dish) => (
    !cheapest || dish.discountPrice < cheapest.discountPrice ? dish : cheapest
  ), undefined);
}

export function getDishDiscountRate(dish?: Dish) {
  if (!dish || dish.price <= 0 || dish.discountPrice >= dish.price) return 0;
  return Math.round((1 - dish.discountPrice / dish.price) * 100);
}

export function formatCheapestDishOffer(store: Pick<Store, 'dishes'>) {
  const dish = getCheapestDish(store);
  if (!dish) return '마감 할인 상품 보기';
  const rate = getDishDiscountRate(dish);
  return `${rate > 0 ? `${rate}% · ` : ''}${dish.discountPrice.toLocaleString()}원부터`;
}
