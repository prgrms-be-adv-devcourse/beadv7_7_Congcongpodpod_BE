import { api } from './api';
import { cachedQuery, invalidateQueries } from './query-cache';
import type { Dish, Store } from '@/types/store';

type Envelope<T> = { data?: T };
type FavoriteDish = {
  dishId: number;
  dishName: string;
  registeredAt?: string;
  description?: string;
  thumbnailUrl?: string | null;
  stockQuantity?: number;
  dishPrice?: number;
  discountPrice?: number;
};
type FavoriteStore = {
  storeId: number;
  memberId?: number;
  storeName: string;
  storeAddress?: string;
  storePhone?: string;
  openTime?: string;
  closeTime?: string;
  status?: string;
  latitude?: number;
  longitude?: number;
  category?: string;
  dishes?: FavoriteDish[];
};

const unwrap = <T,>(value: T | Envelope<T>): T =>
  value && typeof value === 'object' && 'data' in value && value.data !== undefined ? value.data : value as T;

const mapDish = (dish: FavoriteDish): Dish => ({
  dishId: dish.dishId,
  dishName: dish.dishName,
  description: dish.description ?? '',
  price: Number(dish.dishPrice ?? 0),
  discountPrice: Number(dish.discountPrice ?? dish.dishPrice ?? 0),
  quantity: Number(dish.stockQuantity ?? 0),
  imageUrl: dish.thumbnailUrl ?? undefined,
  registeredAt: dish.registeredAt,
});

const mapStore = (store: FavoriteStore): Store => ({
  storeId: store.storeId,
  memberId: store.memberId,
  storeName: store.storeName,
  category: store.category ?? '기타',
  address: store.storeAddress ?? '',
  phone: store.storePhone,
  openTime: store.openTime,
  closeTime: store.closeTime,
  status: store.status,
  latitude: Number(store.latitude ?? 0),
  longitude: Number(store.longitude ?? 0),
  dishes: (store.dishes ?? []).map(mapDish),
});

export async function getFavorites(force = false) {
  return cachedQuery('favorites', async () => unwrap(await api<FavoriteStore[] | Envelope<FavoriteStore[]>>('/favorites', undefined, { globalLoading: false })).map(mapStore), 10_000, force);
}

export async function getFavoriteStatus(storeId: number) {
  return unwrap(await api<{ isFavorite: boolean } | Envelope<{ isFavorite: boolean }>>(`/favorites/${storeId}`, undefined, { globalLoading: false })).isFavorite;
}

export async function addFavorite(storeId: number) {
  await api('/favorites', { method: 'POST', body: JSON.stringify({ storeId }) }, { globalLoading: false });
  invalidateQueries('favorites');
}

export async function removeFavorite(storeId: number) {
  await api(`/favorites/${storeId}`, { method: 'DELETE' }, { globalLoading: false });
  invalidateQueries('favorites');
}
