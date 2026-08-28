import { api } from './api';
import { cachedQuery } from './query-cache';
import type { StoreAvailabilityMode } from '@/providers/store-availability-provider';
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
  pickupStartTime?: string;
  pickupEndTime?: string;
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
  hasAvailableDish?: boolean;
  latitude?: number;
  longitude?: number;
  category?: string;
  thumbnailUrl?: string | null;
  imageUrl?: string | null;
  coverImageUrl?: string | null;
  profileImageUrl?: string | null;
  dishes?: ApiDish[];
  cheapestDish?: ApiDish | null;
};

type AiStoreSearchResult = {
  store: ApiStore;
  totalScore?: number;
  badges?: string[];
  reason?: string | null;
};

export type RecommendedStore = {
  store: Store;
  totalScore: number;
  badges: string[];
  reason: string;
};

type Envelope<T> = { success?: boolean; data?: T };
type Page<T> = { content?: T[]; stores?: T[]; page?: number; totalPages?: number; totalElements?: number } | T[];

const unwrapData = <T,>(value: T | Envelope<T>): T =>
  value && typeof value === 'object' && 'data' in value && value.data !== undefined ? value.data : value as T;

const unwrapList = <T,>(value: Page<T>): T[] =>
  Array.isArray(value) ? value : value.stores ?? value.content ?? [];

async function getAllNearbyStorePages(latitude: number, longitude: number, radiusKm: number, size: number, pickupFilter: StoreAvailabilityMode, signal?: AbortSignal) {
  const fetchPage = async (page: number) => {
    const query = new URLSearchParams({
      latitude: String(latitude), longitude: String(longitude), radiusKm: String(radiusKm),
      pickupFilter, page: String(page), size: String(size),
    });
    return unwrapData(await api<ApiStore[] | Envelope<ApiStore[]>>(`/ai/stores/nearby?${query}`, { signal }));
  };
  const stores: ApiStore[] = [];
  for (let page = 0; page < 200; page += 1) {
    const batch = await fetchPage(page);
    stores.push(...batch);
    if (batch.length < size) break;
  }
  return [...new Map(stores.map(store => [store.storeId, store])).values()];
}

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
  pickupStartTime: dish.pickupStartTime,
  pickupEndTime: dish.pickupEndTime,
  storeId: dish.storeId,
  storeName: dish.storeName,
});

const mapStore = (store: ApiStore, hasAvailableDish?: boolean): Store => ({
  storeId: store.storeId,
  memberId: store.memberId,
  storeName: store.storeName,
  category: store.category ?? '기타',
  address: store.storeAddress ?? '',
  phone: store.storePhone,
  openTime: store.openTime,
  closeTime: store.closeTime,
  status: store.status,
  hasAvailableDish: store.hasAvailableDish ?? hasAvailableDish,
  latitude: Number(store.latitude ?? 0),
  longitude: Number(store.longitude ?? 0),
  coverImageUrl: store.coverImageUrl ?? store.imageUrl ?? store.thumbnailUrl ?? undefined,
  profileImageUrl: store.profileImageUrl ?? store.thumbnailUrl ?? store.imageUrl ?? undefined,
  imageUrl: store.imageUrl ?? store.thumbnailUrl ?? undefined,
  dishes: (store.dishes ?? (store.cheapestDish ? [store.cheapestDish] : [])).map((dish) => mapDish({
    ...dish,
    storeId: dish.storeId ?? store.storeId,
    storeName: dish.storeName ?? store.storeName,
    stockQuantity: dish.stockQuantity ?? (hasAvailableDish ? 1 : 0),
  })),
});

export async function getNearbyStores(latitude: number, longitude: number, radiusKm = 5, size = 60, pickupFilter: StoreAvailabilityMode = 'NOW', signal?: AbortSignal) {
  return (await getAllNearbyStorePages(latitude, longitude, radiusKm, size, pickupFilter, signal))
    .map((store) => mapStore(store, pickupFilter !== 'ALL'));
}

export async function searchStores(keyword: string, latitude: number, longitude: number) {
  return (await searchRecommendedStores(keyword, latitude, longitude)).map((result) => result.store).slice(0, 8);
}

export async function searchRecommendedStores(keyword: string, latitude: number, longitude: number, radiusKm = 5): Promise<RecommendedStore[]> {
  const query = keyword.trim();
  if (!query) return [];
  const results = await api<AiStoreSearchResult[]>('/ai/search', {
    method: 'POST',
    body: JSON.stringify({ query, latitude, longitude, radiusKm }),
  }, { timeoutMs: 30_000 });
  return results.map((result) => ({
    store: mapStore(result.store, true),
    totalScore: Number(result.totalScore ?? 0),
    badges: result.badges ?? [],
    reason: result.reason?.trim() || result.badges?.[0] || '검색 조건과 잘 맞는 픽업 상품이에요.',
  }));
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
