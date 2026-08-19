import { api } from './api';
import type { CustomerOrder } from './orders';

type Envelope<T> = { data?: T };
const unwrap = <T,>(value: T | Envelope<T>): T => value && typeof value === 'object' && 'data' in value && value.data !== undefined ? value.data : value as T;

export type ApiCartItem = {
  cartItemId: number; dishId: number; storeId: number; dishName: string; unitPrice: number; quantity: number;
  subtotalPrice: number; status: string; orderable: boolean;
};
export type ApiCart = { cartId: number; memberId: number; items: ApiCartItem[]; totalPrice: number };

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

export async function createOrderFromCartItem(cartItemId: number) {
  return unwrap(await api<CustomerOrder | Envelope<CustomerOrder>>(`/orders/cartItems/${cartItemId}`, { method: 'POST' }));
}
