import { api } from './api';
import { getStore } from './stores';
import { cachedQuery, invalidateQueries } from './query-cache';

type Envelope<T> = { data?: T };
type Page<T> = { content?: T[]; totalElements?: number };
const unwrap = <T,>(value: T | Envelope<T>): T => value && typeof value === 'object' && 'data' in value && value.data !== undefined ? value.data : value as T;

export type OrderStatus = 'RESERVED' | 'PICKUP_READY' | 'PICKED_UP' | 'NO_SHOW' | 'CANCELLED' | 'REJECTED';
export type CustomerOrder = {
  orderId: number; storeId: number; status: OrderStatus; rejectReason?: string; paymentStatus: string;
  dishId: number; dishName: string; quantity: number; unitPrice: number; totalPrice: number;
  pickupStartAt?: string; pickupEndAt?: string; storeName?: string; storeImageUrl?: string;
};
export type PickupCode = { orderId: number; dishName: string; pickupCode: string; pickupStartAt?: string; pickupEndAt?: string };

export async function getMyOrders(force = false) {
  return cachedQuery('orders:mine', async () => {
  const page = unwrap(await api<Page<CustomerOrder> | Envelope<Page<CustomerOrder>>>('/orders?page=0&size=50'));
  const orders = (page.content ?? []).sort((a, b) => b.orderId - a.orderId);
  const stores = new Map<number, Awaited<ReturnType<typeof getStore>>>();
  await Promise.all([...new Set(orders.map((order) => order.storeId))].map(async (storeId) => {
    try { stores.set(storeId, await getStore(storeId)); } catch { /* 주문은 매장 조회 실패와 무관하게 표시한다. */ }
  }));
  return orders.map((order) => ({ ...order, storeName: stores.get(order.storeId)?.storeName, storeImageUrl: stores.get(order.storeId)?.profileImageUrl ?? stores.get(order.storeId)?.imageUrl }));
  }, 6_000, force);
}

export async function getMyOrderCount() {
  const page = unwrap(await api<Page<CustomerOrder> | Envelope<Page<CustomerOrder>>>('/orders?page=0&size=1'));
  return Number(page.totalElements ?? page.content?.length ?? 0);
}

export async function getOrder(orderId: number) {
  const order = unwrap(await api<CustomerOrder | Envelope<CustomerOrder>>(`/orders/${orderId}`));
  try {
    const store = await getStore(order.storeId);
    return { ...order, storeName: store.storeName, storeImageUrl: store.profileImageUrl ?? store.imageUrl };
  } catch { return order; }
}

export async function getPickupCode(orderId: number) {
  return unwrap(await api<PickupCode | Envelope<PickupCode>>(`/orders/${orderId}/pickupCode`));
}

export async function cancelOrder(orderId: number) {
  const result = unwrap(await api<CustomerOrder | Envelope<CustomerOrder>>(`/orders/${orderId}/cancel`, { method: 'PATCH' }));
  invalidateQueries('orders:');
  return result;
}
