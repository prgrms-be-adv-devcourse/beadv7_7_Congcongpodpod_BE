// 설계 문서 3.2절의 1 VU · 2계정 기능 검증.
// 부하가 아니라 계약과 상태 전이를 확인하는 실행이다. 요청은 최대 50회 안팎이다.
//
//   구매자 seller001 → 매장 2 · 상품 2 주문
//   매장 사용자 seller002 → 재고 조정 → 주문 수락 → 픽업 완료
//
// 자기 매장 주문을 만들지 않기 위해 한 VU가 두 계정의 토큰을 따로 보관한다.
import exec from 'k6/execution';
import { ORDER_WINDOW, businessHourNow, storeIdFor } from './lib/config.js';
import { getRequestCount, resetRequestCount } from './lib/api.js';
import {
  buyerPurchase,
  clearLeftoverCartItem,
  getThinkTotal,
  openSession,
  resetThinkTotal,
  sellerAdjustStock,
  sellerHandleOrder,
} from './lib/flow.js';
import { iterationRequests, sessionDuration, thinkTimeTotal } from './lib/metrics.js';

const BUYER_ACCOUNT = Number(__ENV.BUYER_ACCOUNT || 1);
const SELLER_ACCOUNT = Number(__ENV.SELLER_ACCOUNT || 2);

// 영업 구간 밖에서도 굳이 돌려보고 싶을 때만 1로 둔다.
const IGNORE_ORDER_WINDOW = __ENV.IGNORE_ORDER_WINDOW === '1';

export const options = {
  vus: 1,
  iterations: 1,
  thresholds: {
    checks: ['rate==1'],
    http_req_failed: ['rate==0'],
  },
};

export function setup() {
  const business = businessHourNow();
  console.log('재고 변경 방식: PATCH /dishes/{id}/stock');
  console.log(`구매자 seller${String(BUYER_ACCOUNT).padStart(3, '0')} → 매장 ${storeIdFor(SELLER_ACCOUNT)} 주문`);
  console.log(`현재 영업 시각: ${business.label}`);

  // 시드 매장은 09:00~22:00 영업이고 시드 상품의 픽업 마감이 21:30이다. 그 밖에서는 주문 생성이 실패한다.
  // 실패할 걸 알면서 진행하면 장바구니 항목과 재고 조정만 남기므로 여기서 멈춘다.
  if (!business.orderable && !IGNORE_ORDER_WINDOW) {
    exec.test.abort(
      `${business.label}은 주문 가능 구간(0${ORDER_WINDOW.fromHour}:00~21:30 KST) 밖입니다. ` +
        '주문이 ORDER_STORE_CLOSED 또는 ORDER_PICKUP_DEADLINE_PASSED로 실패하므로 실행하지 않습니다. ' +
        '구간 안에서 다시 실행하거나, 그래도 돌리려면 IGNORE_ORDER_WINDOW=1을 주세요.',
    );
  }
  return {};
}

export default function () {
  resetRequestCount();
  resetThinkTotal();
  const startedAt = Date.now();

  const buyer = openSession(BUYER_ACCOUNT);
  const seller = openSession(SELLER_ACCOUNT);
  clearLeftoverCartItem(buyer);
  clearLeftoverCartItem(seller);

  const order = buyerPurchase(buyer, SELLER_ACCOUNT);
  const store = sellerAdjustStock(seller, 1);
  const handledOrderId = sellerHandleOrder(
    seller,
    store ? store.storeId : storeIdFor(SELLER_ACCOUNT),
  );

  const elapsed = Date.now() - startedAt;
  sessionDuration.add(elapsed);
  thinkTimeTotal.add(getThinkTotal() * 1000);
  iterationRequests.add(getRequestCount());

  console.log('--- 검증 결과 ---');
  console.log(`총 HTTP 호출: ${getRequestCount()}회`);
  console.log(`행동 대기 합계: ${getThinkTotal().toFixed(1)}초`);
  console.log(`세션 시간: ${(elapsed / 1000).toFixed(1)}초`);
  console.log(`생성한 주문: ${order ? order.orderId : '없음'}`);
  console.log(`매장이 처리한 주문: ${handledOrderId || '없음'}`);
  if (order && handledOrderId && order.orderId !== handledOrderId) {
    console.warn(`생성한 주문과 처리한 주문이 다릅니다: ${order.orderId} vs ${handledOrderId}`);
  }
}
