import { createContext, type PropsWithChildren, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';

import { addCartItem, clearMemberCart, getMemberCart, updateCartItemQuantity, type ApiCartItem } from '@/lib/cart';
import { getDish, getStore } from '@/lib/stores';
import { useAuth } from '@/providers/auth-provider';
import type { Dish } from '@/types/store';

export type CartEntry = Dish & {
  cartId: number;
  cartItemId: number;
  storeId?: number;
  storeName: string;
  storeCategory?: string;
  cartQuantity: number;
  orderable: boolean;
  cartStatus: string;
};

type CartContextValue = {
  item: CartEntry | null;
  loading: boolean;
  refresh: () => Promise<void>;
  add: (dish: Dish, storeName: string, storeId?: number) => Promise<void>;
  changeQuantity: (amount: number) => Promise<void>;
  clear: () => Promise<void>;
  resetLocal: () => void;
};

const CartContext = createContext<CartContextValue | null>(null);

async function hydrateItem(cartId: number, serverItem: ApiCartItem): Promise<CartEntry> {
  const dish = await getDish(serverItem.dishId);
  const storeId = serverItem.storeId ?? dish.storeId;
  const store = storeId ? await getStore(storeId).catch(() => null) : null;
  return {
    ...dish,
    cartId,
    cartItemId: serverItem.cartItemId,
    discountPrice: Number(serverItem.unitPrice),
    storeId,
    storeName: store?.storeName ?? dish.storeName ?? '매장',
    storeCategory: store?.category,
    cartQuantity: Number(serverItem.quantity),
    orderable: serverItem.orderable,
    cartStatus: serverItem.status,
  };
}

export function CartProvider({ children }: PropsWithChildren) {
  const { member } = useAuth();
  const [item, setItem] = useState<CartEntry | null>(null);
  const [loading, setLoading] = useState(true);
  const cartIdRef = useRef<number | undefined>(undefined);

  const refresh = useCallback(async () => {
    if (!member) {
      cartIdRef.current = undefined;
      setItem(null);
      return;
    }
    setLoading(true);
    try {
      const cart = await getMemberCart();
      cartIdRef.current = cart.cartId;
      const serverItem = cart.items[0];
      setItem(serverItem ? await hydrateItem(cart.cartId, serverItem) : null);
    } finally {
      setLoading(false);
    }
  }, [member]);

  useEffect(() => { void refresh().catch(() => setItem(null)); }, [refresh]);

  const ensureCartId = useCallback(async () => {
    if (!member) throw new Error('로그인이 필요해요.');
    if (cartIdRef.current) return cartIdRef.current;
    const cart = await getMemberCart();
    cartIdRef.current = cart.cartId;
    return cart.cartId;
  }, [member]);

  const add = useCallback(async (dish: Dish, storeName: string, storeId?: number) => {
    const cartId = await ensureCartId();
    const quantity = item?.dishId === dish.dishId ? item.cartQuantity + 1 : 1;
    const serverItem = await addCartItem(cartId, dish.dishId, quantity);
    setItem({ ...dish, cartId, cartItemId: serverItem.cartItemId, discountPrice: Number(serverItem.unitPrice), storeId, storeName, storeCategory: dish.storeCategory, cartQuantity: Number(serverItem.quantity), orderable: serverItem.orderable, cartStatus: serverItem.status });
  }, [ensureCartId, item]);

  const changeQuantity = useCallback(async (amount: number) => {
    if (!item) return;
    const quantity = Math.max(1, Math.min(item.cartQuantity + amount, Math.max(item.quantity, 1)));
    if (quantity === item.cartQuantity) return;
    const previous = item;
    setItem({ ...item, cartQuantity: quantity });
    try {
      const serverItem = await updateCartItemQuantity(item.cartId, item.cartItemId, quantity);
      setItem((current) => current?.cartItemId === item.cartItemId ? { ...current, cartQuantity: Number(serverItem.quantity), discountPrice: Number(serverItem.unitPrice), orderable: serverItem.orderable, cartStatus: serverItem.status } : current);
    } catch (error) {
      setItem(previous);
      throw error;
    }
  }, [item]);

  const clear = useCallback(async () => {
    if (!item) return;
    const previous = item;
    setItem(null);
    try {
      await clearMemberCart(item.cartId);
    } catch (error) {
      setItem(previous);
      throw error;
    }
  }, [item]);

  const resetLocal = useCallback(() => setItem(null), []);

  const value = useMemo(() => ({ item, loading, refresh, add, changeQuantity, clear, resetLocal }), [item, loading, refresh, add, changeQuantity, clear, resetLocal]);
  return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
}

export function useCart() {
  const value = useContext(CartContext);
  if (!value) throw new Error('useCart must be used inside CartProvider');
  return value;
}
