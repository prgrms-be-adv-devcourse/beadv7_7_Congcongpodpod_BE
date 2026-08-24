import { api } from './api';
import { cachedQuery } from './query-cache';
import type { Dish, Store } from '@/types/store';

type ApiDish = {
  dishId: number;
  dishName: string;
  description?: string;
  dishPrice?: number;
  discountPrice?: number;
  stockQuantity?: number;
  dishStatus?: string;
  thumbnailUrl?: string | null;
  registeredAt?: string;
  storeId?: number;
  storeName?: string;
};

type ApiStore = {
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
  thumbnailUrl?: string | null;
  imageUrl?: string | null;
  coverImageUrl?: string | null;
  profileImageUrl?: string | null;
  dishes?: ApiDish[];
};

type Envelope<T> = { success?: boolean; data?: T };
type Page<T> = { content?: T[]; stores?: T[] } | T[];

const unwrapData = <T,>(value: T | Envelope<T>): T =>
  value && typeof value === 'object' && 'data' in value && value.data !== undefined ? value.data : value as T;

const unwrapList = <T,>(value: Page<T>): T[] =>
  Array.isArray(value) ? value : value.stores ?? value.content ?? [];

const mapDish = (dish: ApiDish): Dish => ({
  dishId: dish.dishId,
  dishName: dish.dishName,
  description: dish.description ?? '',
  price: Number(dish.dishPrice ?? 0),
  discountPrice: Number(dish.discountPrice ?? dish.dishPrice ?? 0),
  quantity: Number(dish.stockQuantity ?? 0),
  status: dish.dishStatus,
  imageUrl: dish.thumbnailUrl ?? undefined,
  registeredAt: dish.registeredAt,
  storeId: dish.storeId,
  storeName: dish.storeName,
});

const mapStore = (store: ApiStore): Store => ({
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
  coverImageUrl: store.coverImageUrl ?? store.imageUrl ?? store.thumbnailUrl ?? undefined,
  profileImageUrl: store.profileImageUrl ?? store.thumbnailUrl ?? store.imageUrl ?? undefined,
  imageUrl: store.imageUrl ?? store.thumbnailUrl ?? undefined,
  dishes: (store.dishes ?? []).map(mapDish),
});

export async function getNearbyStores(latitude: number, longitude: number, radiusKm = 5, size = 60, signal?: AbortSignal) {
  const query = new URLSearchParams({
    latitude: String(latitude), longitude: String(longitude), radiusKm: String(radiusKm), page: '0', size: String(size),
  });
  const result = unwrapData(await api<Page<ApiStore> | Envelope<Page<ApiStore>>>(`/stores/nearby?${query}`, { signal }));
  return unwrapList(result).map(mapStore);
}

export async function searchStores(keyword: string, latitude: number, longitude: number) {
  const normalized = keyword.trim().toLocaleLowerCase('ko');
  if (!normalized) return [];
  const query = new URLSearchParams({
    latitude: String(latitude), longitude: String(longitude), radiusKm: '500', page: '0', size: '100',
  });
  const result = unwrapData(await api<Page<ApiStore> | Envelope<Page<ApiStore>>>(`/stores/nearby?${query}`));
  return unwrapList(result)
    .map(mapStore)
    .filter((store) => [store.storeName, store.category, store.address]
      .some((value) => value.toLocaleLowerCase('ko').includes(normalized)))
    .slice(0, 8);
}

export async function getStore(storeId: number, force = false) {
  return cachedQuery(`store:${storeId}`, async () => mapStore(unwrapData(await api<ApiStore | Envelope<ApiStore>>(`/stores/${storeId}`))), 20_000, force);
}

export async function getStoreDishes(storeId: number, force = false) {
  return cachedQuery(`store-dishes:${storeId}`, async () => {
    const result = unwrapData(await api<Page<ApiDish> | Envelope<Page<ApiDish>>>(`/dishes?storeId=${storeId}`));
    return unwrapList(result).map(mapDish);
  }, 10_000, force);
}

export async function getDish(dishId: number, force = false) {
  return cachedQuery(`dish:${dishId}`, async () => mapDish(unwrapData(await api<ApiDish | Envelope<ApiDish>>(`/dishes/${dishId}`))), 10_000, force);
}
