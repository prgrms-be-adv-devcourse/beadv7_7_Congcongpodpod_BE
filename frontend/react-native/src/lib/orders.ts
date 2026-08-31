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

// 주문 목록. 매장 정보는 주문 응답에 실려 오므로 매장을 따로 부르지 않는다.
// 예전에는 매장마다 getStore를 불렀는데, 최근 50건에 서로 다른 매장이 50개까지 들어가
// 화면 한 번에 요청이 51개 나갔다(2026-08-29 부하 실측: 전체 요청의 79%, 8.19초).
// storeName은 서버가 주문 응답에 담아 주고, 매장 이미지는 서버에 필드 자체가 없어
// 그 조회로도 undefined였다 — 화면은 이미 폴백을 갖고 있다.
export async function getMyOrders(force = false) {
  return cachedQuery('orders:mine', async () => {
    const page = unwrap(await api<Page<CustomerOrder> | Envelope<Page<CustomerOrder>>>('/orders?page=0&size=50'));
    return (page.content ?? []).sort((a, b) => b.orderId - a.orderId);
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
