import { api } from './api';
import type { CustomerOrder } from './orders';

type Envelope<T> = { data?: T };
const unwrap = <T,>(value: T | Envelope<T>): T => value && typeof value === 'object' && 'data' in value && value.data !== undefined ? value.data : value as T;

export type ApiCartItem = {
  cartItemId: number; dishId: number; storeId?: number; dishName: string; unitPrice: number; quantity: number;
  subtotalPrice: number; status: string; orderable: boolean; lastAppliedDishPriceVersion: number;
};
export type ApiCart = { cartId: number; memberId: number; items: ApiCartItem[]; totalPrice: number };

export type CartAvailability = { orderable: boolean; label?: '품절' | '수량 부족' | '판매 종료'; description?: string };

export function getCartAvailability(item: Pick<ApiCartItem, 'status' | 'orderable' | 'quantity'> & { stockQuantity?: number }): CartAvailability {
  if (item.status === 'OUT_OF_STOCK') return { orderable: false, label: '품절', description: '재고가 다시 준비되면 주문할 수 있어요.' };
  if (item.status === 'INSUFFICIENT_STOCK') return { orderable: false, label: '수량 부족', description: '구매 수량을 현재 재고 이하로 조정해주세요.' };
  if (item.status === 'DISH_UNAVAILABLE' || !item.orderable) return { orderable: false, label: '판매 종료', description: '판매가 종료되어 지금은 주문할 수 없어요.' };
  if (item.stockQuantity !== undefined && item.stockQuantity <= 0) return { orderable: false, label: '품절', description: '재고가 다시 준비되면 주문할 수 있어요.' };
  if (item.stockQuantity !== undefined && item.quantity > item.stockQuantity) return { orderable: false, label: '수량 부족', description: `현재 ${item.stockQuantity}개까지 주문할 수 있어요.` };
  return { orderable: true };
}

export async function getMemberCart() {
  return unwrap(await api<ApiCart | Envelope<ApiCart>>('/carts/members'));
}

export async function putCartItem(dishId: number, quantity: number) {
  const cart = await getMemberCart();
  const item = unwrap(await api<ApiCartItem | Envelope<ApiCartItem>>(`/carts/${cart.cartId}/items`, {
    method: 'POST', body: JSON.stringify({ dishId, quantity }),
  }));
  return { cart, item };
}

export async function addCartItem(cartId: number, dishId: number, quantity: number) {
  return unwrap(await api<ApiCartItem | Envelope<ApiCartItem>>(`/carts/${cartId}/items`, {
    method: 'POST', body: JSON.stringify({ dishId, quantity }),
  }));
}

export async function updateCartItemQuantity(cartId: number, cartItemId: number, quantity: number) {
  return unwrap(await api<ApiCartItem | Envelope<ApiCartItem>>(`/carts/${cartId}/items/${cartItemId}`, {
    method: 'PATCH', body: JSON.stringify({ quantity }),
  }));
}

export async function removeCartItem(cartId: number, cartItemId: number) {
  await api<void>(`/carts/${cartId}/items/${cartItemId}`, { method: 'DELETE' });
}

export async function clearMemberCart(cartId: number) {
  await api<void>(`/carts/${cartId}`, { method: 'DELETE' });
}

export async function createOrderFromCartItem(cartItemId: number, dishPriceVersion: number, usedPoint = 0) {
  return unwrap(await api<CustomerOrder | Envelope<CustomerOrder>>(`/orders/cartItems/${cartItemId}`, {
    method: 'POST',
    body: JSON.stringify({ dishPriceVersion, usedPoint }),
  }));
}
