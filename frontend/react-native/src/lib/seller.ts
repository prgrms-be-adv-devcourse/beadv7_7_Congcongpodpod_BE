import { api } from './api';
import type { Dish, Store } from '@/types/store';

type Envelope<T> = { data?: T };
type Page<T> = { content?: T[] };
const unwrap = <T,>(value: T | Envelope<T>): T => value && typeof value === 'object' && 'data' in value && value.data !== undefined ? value.data : value as T;

type RawStore = { storeId: number; memberId?: number; storeName: string; businessNumber?: string; storeAddress?: string; storePhone?: string; openTime?: string; closeTime?: string; status?: string; latitude?: number; longitude?: number; category?: string; imageUrl?: string; thumbnailUrl?: string; coverImageUrl?: string; profileImageUrl?: string };
type RawDish = { dishId: number; storeId?: number; dishName: string; description?: string; dishPrice?: number; discountPrice?: number; stockQuantity?: number; dishStatus?: string; thumbnailUrl?: string; registeredAt?: string };
export type SellerOrder = { orderId: number; storeId: number; status: string; dishName: string; quantity: number; totalPrice: number; pickupStartAt?: string; pickupEndAt?: string; memberName?: string; phone?: string; pickupCode?: string };
export type SellerOrderAccept = { orderId: number; pickUpCode: string };
export type StoreSales = { storeId: number; salesDate: string; salesAmount: number };
export type Settlement = { settlementId: number; storeId: number; settlementMonth: string; orderCount: number; grossAmount: number; feeAmount: number; settlementAmount: number; status: string };
export type StoreRegistration = { storeName: string; businessNumber: string; storeAddress: string; storePhone: string; openTime: string; closeTime: string; latitude: number; longitude: number; category: string; holidays: string[] };

const mapStore = (x: RawStore): Store => ({ storeId: x.storeId, memberId: x.memberId, storeName: x.storeName, businessNumber: x.businessNumber, category: x.category ?? '기타', address: x.storeAddress ?? '', phone: x.storePhone, openTime: x.openTime, closeTime: x.closeTime, status: x.status, latitude: Number(x.latitude ?? 0), longitude: Number(x.longitude ?? 0), coverImageUrl: x.coverImageUrl ?? x.imageUrl, profileImageUrl: x.profileImageUrl ?? x.thumbnailUrl ?? x.imageUrl, imageUrl: x.imageUrl ?? x.thumbnailUrl, dishes: [] });
const mapDish = (x: RawDish): Dish => ({ dishId: x.dishId, storeId: x.storeId, dishName: x.dishName, description: x.description ?? '', price: Number(x.dishPrice ?? 0), discountPrice: Number(x.discountPrice ?? x.dishPrice ?? 0), quantity: Number(x.stockQuantity ?? 0), status: x.dishStatus, imageUrl: x.thumbnailUrl, registeredAt: x.registeredAt });

export async function getMyStores() { return unwrap(await api<RawStore[] | Envelope<RawStore[]>>('/stores/mine')).map(mapStore); }
export async function registerStore(payload: StoreRegistration) { return mapStore(unwrap(await api<RawStore | Envelope<RawStore>>('/stores', { method: 'POST', body: JSON.stringify(payload) }))); }
export async function updateStore(storeId: number, payload: Omit<StoreRegistration, 'businessNumber'>) { return mapStore(unwrap(await api<RawStore | Envelope<RawStore>>(`/stores/${storeId}`, { method: 'PUT', body: JSON.stringify(payload) }))); }
export async function changeStoreStatus(storeId: number, status: 'OPEN' | 'CLOSED') { return mapStore(unwrap(await api<RawStore | Envelope<RawStore>>(`/stores/${storeId}/status`, { method: 'PATCH', body: JSON.stringify({ status }) }))); }
export async function getSellerDishes(storeId: number): Promise<Dish[]> {
  try {
    return unwrap(await api<RawDish[] | Envelope<RawDish[]>>(`/stores/${storeId}/dishes`)).map(mapDish);
  } catch {
    return [];
  }
}
export async function adjustDishStock(dishId: number, quantityDelta: number) { return mapDish(unwrap(await api<RawDish | Envelope<RawDish>>(`/dishes/${dishId}/stock`, { method: 'PATCH', body: JSON.stringify({ quantityDelta }) }))); }
export async function getStoreOrders(storeId: number, status?: string) { const q = status ? `?status=${status}` : ''; const page = unwrap(await api<Page<SellerOrder> | Envelope<Page<SellerOrder>>>(`/orders/stores/${storeId}${q}`)); return page.content ?? []; }
export async function getStoreSales(storeId: number) { const result = unwrap(await api<StoreSales | Envelope<StoreSales>>(`/orders/stores/${storeId}/sales`)); return { ...result, salesAmount: Number(result.salesAmount) }; }
export async function acceptStoreOrder(orderId: number) { return unwrap(await api<SellerOrderAccept | Envelope<SellerOrderAccept>>(`/orders/${orderId}/accept`, { method: 'POST' })); }
export async function rejectStoreOrder(orderId: number, reason: 'OUT_OF_STOCK'|'QUALITY_ISSUE'|'NOT_READY'|'STORE_CLOSED') { return unwrap(await api<{orderId:number;status:string;rejectReason:string} | Envelope<{orderId:number;status:string;rejectReason:string}>>(`/orders/${orderId}/reject`, { method: 'POST', body: JSON.stringify({ reason }) })); }
export async function updateStorePickup(orderId: number, status: 'PICKED_UP'|'NO_SHOW') { return unwrap(await api<{orderId:number;status:string} | Envelope<{orderId:number;status:string}>>(`/orders/${orderId}/pickup`, { method: 'PATCH', body: JSON.stringify({ status }) })); }
export async function getSettlements() {
  const page = unwrap(await api<Page<Settlement> | Envelope<Page<Settlement>>>('/settlements?page=0&size=24'));
  return (page.content ?? []).map((settlement) => ({
    ...settlement,
    orderCount: Number(settlement.orderCount),
    grossAmount: Number(settlement.grossAmount),
    feeAmount: Number(settlement.feeAmount),
    settlementAmount: Number(settlement.settlementAmount),
  }));
}
