import { invalidateQueries } from './query-cache';

export type OrderStateEvent = { type: string; orderId?: number | null };
type Listener = (event: OrderStateEvent) => void;

const listeners = new Set<Listener>();

export function notifyOrderStateChanged(event: OrderStateEvent) {
  invalidateQueries('orders:');
  invalidateQueries('seller:orders:');
  listeners.forEach(listener => listener(event));
}

export function subscribeOrderStateChanged(listener: Listener) {
  listeners.add(listener);
  return () => { listeners.delete(listener); };
}
