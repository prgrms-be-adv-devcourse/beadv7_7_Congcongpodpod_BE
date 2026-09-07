// 전역 VU 상한 안에서 두 VU가 같은 구매 계정을 잡으면 안 된다.
//
// ladder-config.js는 구매 VU를 150개로 묶으면 겹침이 막힌다고 보고 MAX_PURCHASE_VUS를
// 두었다. 그러나 계정을 정하는 __VU는 시나리오 안 순번이 아니라 테스트 전역에서 배정된
// 번호다. 배경 시나리오(조회 14 + 판매자 5 + 재고 1)가 앞자리를 쓰면 구매 VU의 번호가
// 그만큼 뒤로 밀려 대역을 넘는다.
//
// 2026-08-31 실측(420건/분, 전역 167): VU 16과 166이 seller0016을 함께 잡았고,
// 그 실행의 주문 거절 16건이 전부 그 계정 하나에서 났다. 계정 하나에 장바구니 항목이
// 하나뿐이라(CartService.addItem의 upsert) 한쪽이 담은 항목을 다른 쪽이 덮어쓴다.
import { check } from 'k6';

import { seedAccountNoForVu } from '../lib/accounts.js';
import { BUYER_ACCOUNT_POOLS } from '../lib/config.js';
import { assertBuyerPoolsFit, totalVusFor } from '../lib/ladder-config.js';

export const options = {
  vus: 1,
  iterations: 1,
  thresholds: {
    checks: ['rate==1'],
  },
};

// 1번부터 vuMax번까지 훑어 같은 계정을 두 번 잡는 첫 지점을 찾는다. 없으면 null이다.
function firstCollision(pool, vuMax) {
  const seenBy = {};
  for (let vu = 1; vu <= vuMax; vu += 1) {
    const accountNo = seedAccountNoForVu(pool.start, pool.count, vu);
    if (seenBy[accountNo]) {
      return { accountNo, firstVu: seenBy[accountNo], secondVu: vu };
    }
    seenBy[accountNo] = vu;
  }
  return null;
}

// 두 대역이 한 계정이라도 공유하면 조회 VU가 구매 VU의 장바구니를 건드릴 수 있다.
function poolsOverlap(a, b) {
  return a.start <= b.start + b.count - 1 && b.start <= a.start + a.count - 1;
}

export default function () {
  // 오늘까지 실측에 쓴 도착률. 새 도착률을 쓰기 시작하면 여기에 추가한다.
  const orderRates = [240, 300, 360, 390, 420];

  for (const orderRate of orderRates) {
    const vuMax = totalVusFor(orderRate);

    // 구매와 조회 둘 다 본다. 조회 VU도 전역 번호를 나누므로 대역이 좁으면 똑같이 겹친다.
    for (const name of Object.keys(BUYER_ACCOUNT_POOLS)) {
      const collision = firstCollision(BUYER_ACCOUNT_POOLS[name], vuMax);
      const detail = collision
        ? `계정 ${collision.accountNo}을 VU ${collision.firstVu}와 ${collision.secondVu}가 함께 잡는다`
        : '없음';

      check(collision, {
        [`${orderRate}건/분(전역 VU ${vuMax}) ${name}: 계정이 겹치지 않는다 — ${detail}`]: (
          found,
        ) => found === null,
      });
    }

    // 실행 시작 검사가 같은 조건에서 통과해야 한다. 던지면 그 자체가 실패다.
    let assertionError = null;
    try {
      assertBuyerPoolsFit(vuMax);
    } catch (error) {
      assertionError = String(error);
    }
    check(assertionError, {
      [`${orderRate}건/분: 시작 검사가 통과한다 — ${assertionError || '통과'}`]: (e) => e === null,
    });
  }

  check(BUYER_ACCOUNT_POOLS, {
    '구매 대역과 조회 대역이 겹치지 않는다': (pools) =>
      !poolsOverlap(pools.purchase, pools.browse),
  });
}
