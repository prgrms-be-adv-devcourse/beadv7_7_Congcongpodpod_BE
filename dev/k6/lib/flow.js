// 화면 흐름 단위 함수. 설계 문서 4.2절의 호출 수와 6.1절의 대기 12곳을 그대로 옮긴 것이다.
// 부하 스크립트와 검증 스크립트가 같은 함수를 써야 "검증한 코드와 부하를 준 코드가 다른" 문제가 생기지 않는다.
import { sleep } from 'k6';
import {
  PAGE,
  SEED,
  SELLER_ORDER_RETRY,
  SELLER_ORDER_RETRY_WAIT,
  THINK_MAX,
  THINK_MIN,
} from './config.js';
import { loginWithCredentials, seedCredentials } from './accounts.js';
import { apiBatchGet, apiGet, apiSend, dataOf } from './api.js';
import * as metrics from './metrics.js';
import { selectOldestNewReservedOrder } from './order-selection.js';
import { purchaseTargetOf } from './run-state.js';

let thinkTotal = 0;

export function resetThinkTotal() {
  thinkTotal = 0;
}

export function getThinkTotal() {
  return thinkTotal;
}

// 사용자 클릭·화면 전환 사이의 대기. HTTP 요청 사이에는 쉬지 않는다 (설계 문서 2절).
export function think() {
  const seconds = THINK_MIN + Math.random() * (THINK_MAX - THINK_MIN);
  thinkTotal += seconds;
  sleep(seconds);
}

const unique = (values) => Array.from(new Set(values));

// 로그인 1회 + 내 정보 + 장바구니 회원 정보. VU당 최초 1회만 실행한다 (설계 문서 4.1절).
export function openSession(accountNo) {
  return loginWithCredentials(seedCredentials(accountNo));
}

// 이전 실행이 남긴 장바구니 항목을 비운다. 검증 스크립트 전용이라 반복 호출 수에 넣지 않는다.
export function clearLeftoverCartItem(session) {
  if (!session.cartItems || session.cartItems.length === 0) {
    return false;
  }
  console.warn(`[${session.email}] 이전 실행이 남긴 장바구니 항목 ${session.cartItems.length}건을 비웁니다.`);
  apiSend('cart_cleanup', 'DELETE', `/carts/${session.cartId}`, session.token, null);
  session.cartItems = [];
  return true;
}

function findCartItem(cart, dishId) {
  const items = (cart && cart.items) || [];
  for (const item of items) {
    if (item.dishId === dishId) {
      return item;
    }
  }
  return items.length > 0 ? items[0] : null;
}

// 주문 목록 화면: 목록 1회 + 서로 다른 매장 U회(병렬) + PICKUP_READY 주문의 픽업 코드 P회(병렬).
// RN이 Promise.all로 보내는 구간이라 http.batch로 옮겼다 (설계 문서 5절).
function loadOrderList(session) {
  const page = dataOf(apiGet('order_list', `/orders?page=0&size=${PAGE.myOrdersSize}`, session.token));
  const orders = (page && page.content) || [];

  const storeIds = unique(orders.map((order) => order.storeId));
  apiBatchGet('order_list_stores_batch', storeIds.map((id) => `/stores/${id}`), session.token);

  const pickupReady = orders.filter((order) => order.status === 'PICKUP_READY');
  apiBatchGet(
    'order_pickup_codes_batch',
    pickupReady.map((order) => `/orders/${order.orderId}/pickupCode`),
    session.token,
  );

  const calls = 1 + storeIds.length + pickupReady.length;
  metrics.orderListCalls.add(calls);
  metrics.orderListStoreFanout.add(storeIds.length);
  metrics.orderListPickupCodeCalls.add(pickupReady.length);
  return calls;
}

// 구매 동선: 주변 매장 → 매장 상세 → 상품 상세 → 장바구니 → 주문 → 주문 목록.
// 대기 1~6번이 여기에 있다.
export function buyerPurchase(session, target) {
  const { storeId, dishId } = purchaseTargetOf(target);

  apiGet(
    'store_nearby',
    `/stores/nearby?latitude=${SEED.latitude}&longitude=${SEED.longitude}&radiusKm=${PAGE.nearbyRadiusKm}&page=0&size=${PAGE.nearbySize}`,
    session.token,
  );
  think(); // 1. 홈에서 매장 선택

  // RN stores/[storeId].tsx는 Promise.all([getStore, getStoreDishes])로 동시에 보낸다.
  apiBatchGet('store_detail_batch', [`/stores/${storeId}`, `/dishes?storeId=${storeId}`], session.token);
  think(); // 2. 매장에서 상품 선택

  // RN dishes/[dishId].tsx는 상품을 먼저 받고 그 storeId로 매장을 받는다 — 직렬 2회다.
  apiGet('dish_detail', `/dishes/${dishId}`, session.token);
  apiGet('dish_detail_store', `/stores/${storeId}`, session.token);
  think(); // 3. 상품 수량 결정

  const added = dataOf(
    apiSend('cart_add', 'POST', `/carts/${session.cartId}/items`, session.token, { dishId, quantity: 1 }),
  );
  think(); // 4. 장바구니 이동

  apiGet('deposit_balance', '/deposits/balance', session.token);
  think(); // 5. 주문 화면 이동

  const revalidated = dataOf(apiGet('cart_revalidate', '/carts/members', session.token));
  think(); // 6. 주문 확정

  const cartItem = findCartItem(revalidated, dishId) || added;
  if (!cartItem || !cartItem.cartItemId) {
    console.error(`[${session.email}] 장바구니 항목을 찾지 못해 주문을 건너뜁니다.`);
    return null;
  }

  // dishPriceVersion은 장바구니 응답의 lastAppliedDishPriceVersion을 그대로 보낸다.
  const dishPriceVersion =
    cartItem.lastAppliedDishPriceVersion === null || cartItem.lastAppliedDishPriceVersion === undefined
      ? 0
      : cartItem.lastAppliedDishPriceVersion;

  const order = dataOf(
    apiSend('order_create', 'POST', `/orders/cartItems/${cartItem.cartItemId}`, session.token, {
      dishPriceVersion,
    }),
  );

  apiGet('cart_after_order', '/carts/members', session.token);
  loadOrderList(session);

  if (order && order.orderId) {
    metrics.ordersCreated.add(1);
    return order;
  }
  return null;
}

// 매장 상품 화면: 내 매장 → 상품 목록 → 재고 조정. 대기 7~9번이 여기에 있다.
export function sellerAdjustStock(session, iterationIndex) {
  think(); // 7. 주문 내역 확인 후 매장 상품 화면 이동

  const stores = dataOf(apiGet('seller_stores_for_dish', '/stores/mine', session.token)) || [];
  const store = stores.length > 0 ? stores[0] : null;
  if (!store) {
    metrics.dishStockAdjustSkipped.add(1);
    console.error(`[${session.email}] 내 매장이 없어 재고 조정을 건너뜁니다.`);
    return null;
  }
  session.storeId = store.storeId;

  // RN은 /dishes/management를 먼저 시도하지만 백엔드에 없어 이 목록만 쓴다 (설계 문서 2절).
  const dishes = dataOf(apiGet('seller_dish_list', `/dishes?storeId=${store.storeId}`, session.token)) || [];
  think(); // 8. 상품 수정 화면 열기

  const dish = dishes.length > 0 ? dishes[0] : null;
  if (!dish) {
    metrics.dishStockAdjustSkipped.add(1);
    console.error(`[${session.email}] 매장 ${store.storeId}에 상품이 없어 재고 조정을 건너뜁니다.`);
    think(); // 9. 상품 수정 저장 — 호출은 없어도 대기 수는 유지한다
    return store;
  }

  // 홀수 반복 +1, 짝수 반복 -1. 의도한 순증감을 0에 가깝게 유지한다 (설계 문서 8.3절).
  const delta = iterationIndex % 2 === 1 ? 1 : -1;

  apiSend('dish_stock_adjust', 'PATCH', `/dishes/${dish.dishId}/stock`, session.token, {
    quantityDelta: delta,
  });

  if (delta > 0) {
    metrics.dishStockDeltaUp.add(delta);
  } else {
    metrics.dishStockDeltaDown.add(-delta);
  }

  think(); // 9. 상품 수정 저장
  return store;
}

// 조건에 맞는 새 주문만 고른다: RESERVED + 시드 주문 구간 밖. 구매자 VU와 직접 연결하지 않는다.
function findNewReservedOrder(session, storeId) {
  const startedAt = Date.now();
  let retries = 0;

  for (let attempt = 0; attempt <= SELLER_ORDER_RETRY; attempt += 1) {
    if (attempt > 0) {
      retries += 1;
      sleep(SELLER_ORDER_RETRY_WAIT);
    }

    const page = dataOf(
      apiGet('seller_orders_reserved', `/orders/stores/${storeId}?status=RESERVED`, session.token),
    );
    const rows = (page && page.content) || [];
    const target = selectOldestNewReservedOrder(rows, SEED.newOrderIdMin);

    if (target) {
      metrics.sellerOrderRetries.add(retries);
      metrics.sellerOrderDiscovery.add(Date.now() - startedAt);
      return target;
    }
  }

  metrics.sellerOrderRetries.add(retries);
  metrics.sellerOrderNotFound.add(1);
  console.error(
    `[${session.email}] 매장 ${storeId}에서 새 주문(RESERVED, id>${SEED.newOrderIdMin})을 찾지 못했습니다.`,
  );
  return null;
}

// 매장 주문 화면: 내 매장 → 예약 주문 → 수락 → 픽업 대기 → 픽업 완료. 대기 10~12번이 여기에 있다.
export function sellerHandleOrder(session, knownStoreId) {
  think(); // 10. 매장 주문 화면 이동

  let storeId = knownStoreId;
  const stores = dataOf(apiGet('seller_stores_for_order', '/stores/mine', session.token)) || [];
  if (stores.length > 0) {
    storeId = stores[0].storeId;
    session.storeId = storeId;
  }
  if (!storeId) {
    metrics.sellerOrderNotFound.add(1);
    console.error(`[${session.email}] 내 매장이 없어 매장 주문 처리를 건너뜁니다.`);
    return null;
  }

  const target = findNewReservedOrder(session, storeId);
  if (!target) {
    return null;
  }
  think(); // 11. 주문 수락

  const accepted = dataOf(apiSend('order_accept', 'POST', `/orders/${target.orderId}/accept`, session.token, null));
  if (accepted) {
    metrics.ordersAccepted.add(1);
  }

  apiGet('seller_orders_pickup_ready', `/orders/stores/${storeId}?status=PICKUP_READY`, session.token);
  think(); // 12. 픽업 완료

  const pickedUp = dataOf(
    apiSend('order_pickup', 'PATCH', `/orders/${target.orderId}/pickup`, session.token, { status: 'PICKED_UP' }),
  );
  if (pickedUp) {
    metrics.ordersPickedUp.add(1);
  }

  return target.orderId;
}
