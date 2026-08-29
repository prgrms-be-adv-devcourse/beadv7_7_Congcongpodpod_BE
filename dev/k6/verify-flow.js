// 설계 문서 3.2절의 1 VU · 2계정 기능 검증.
// 부하가 아니라 계약과 상태 전이를 확인하는 실행이다. 요청은 최대 50회 안팎이다.
//
//   구매자 seller001 → 매장 2 · 상품 2 주문
//   매장 사용자 seller002 → 재고 조정 → 주문 수락 → 픽업 완료
//
// 자기 매장 주문을 만들지 않기 위해 한 VU가 두 계정의 토큰을 따로 보관한다.
//
// 주기를 두 번 돈다. 판매자는 방금 수락한 주문을 픽업하지 않으므로(실제 서비스에서
// PICKUP_READY는 손님이 올 때까지 유지된다), 1회차는 수락만 하고 2회차에서 1회차 주문을
// 픽업한다. 한 번만 돌면 PICKED_UP 전이를 한 번도 확인하지 못한다.
import { check } from 'k6';
import exec from 'k6/execution';
import { ORDER_WINDOW, businessHourNow, dishIdFor, storeIdFor } from './lib/config.js';
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
  console.log(`구매자 seller${String(BUYER_ACCOUNT).padStart(4, '0')} → 매장 ${storeIdFor(SELLER_ACCOUNT)} 주문`);
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

  const target = { storeId: storeIdFor(SELLER_ACCOUNT), dishId: dishIdFor(SELLER_ACCOUNT) };

  const firstOrder = buyerPurchase(buyer, target);
  const store = sellerAdjustStock(seller, 1);
  const storeId = store ? store.storeId : storeIdFor(SELLER_ACCOUNT);
  const firstRound = sellerHandleOrder(seller, storeId);

  // 2회차: 새 주문을 수락하면서 1회차에서 수락해 둔 주문을 픽업한다.
  clearLeftoverCartItem(buyer);
  const secondOrder = buyerPurchase(buyer, target);
  const secondRound = sellerHandleOrder(seller, storeId);

  /*
   * 검사는 "빈 상태에서 시작한다"를 가정하지 않는다. 이전 실행이 PICKUP_READY 주문을 남기므로
   * 1회차가 그것을 픽업하는 것이 정상이다(가장 오래된 것부터 처리한다).
   * 어느 경우에나 성립하는 불변식은 "방금 수락한 주문은 픽업하지 않는다"이다.
   */
  check(null, {
    '1회차: 주문을 수락한다': () => Boolean(firstRound.acceptedOrderId),
    '1회차: 방금 수락한 주문은 픽업하지 않는다': () =>
      firstRound.pickedUpOrderId !== firstRound.acceptedOrderId,
    '2회차: 새 주문을 수락한다': () => Boolean(secondRound.acceptedOrderId),
    '2회차: 픽업까지 진행된다': () => Boolean(secondRound.pickedUpOrderId),
    '2회차: 방금 수락한 주문은 픽업하지 않는다': () =>
      secondRound.pickedUpOrderId !== secondRound.acceptedOrderId,
  });

  // 남은 주문이 없던 환경에서는 2회차가 1회차 수락분을 집는다. 있으면 그보다 오래된 것이 먼저다.
  if (secondRound.pickedUpOrderId !== firstRound.acceptedOrderId) {
    console.log(
      `2회차 픽업 대상이 1회차 수락분(${firstRound.acceptedOrderId})이 아닙니다: ` +
        `${secondRound.pickedUpOrderId}. 이전 실행이 남긴 주문이 먼저 처리된 것으로 보입니다.`,
    );
  }

  const elapsed = Date.now() - startedAt;
  sessionDuration.add(elapsed);
  thinkTimeTotal.add(getThinkTotal() * 1000);
  iterationRequests.add(getRequestCount());

  console.log('--- 검증 결과 ---');
  console.log(`총 HTTP 호출: ${getRequestCount()}회`);
  console.log(`행동 대기 합계: ${getThinkTotal().toFixed(1)}초`);
  console.log(`세션 시간: ${(elapsed / 1000).toFixed(1)}초`);
  console.log(`생성한 주문: ${firstOrder ? firstOrder.orderId : '없음'} / ${secondOrder ? secondOrder.orderId : '없음'}`);
  console.log(`1회차 — 수락 ${firstRound.acceptedOrderId || '없음'}, 픽업 ${firstRound.pickedUpOrderId || '없음(정상)'}`);
  console.log(`2회차 — 수락 ${secondRound.acceptedOrderId || '없음'}, 픽업 ${secondRound.pickedUpOrderId || '없음'}`);

  // 2회차 수락분은 PICKUP_READY로 남는다. 다음 실행이 픽업하거나, 픽업 마감 스케줄러가 정리한다.
  if (secondRound.acceptedOrderId) {
    console.log(`PICKUP_READY로 남는 주문: ${secondRound.acceptedOrderId}`);
  }
}
