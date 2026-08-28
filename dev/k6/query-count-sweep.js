// 설계 문서 §6 실행 순서 2번 — 1 VU로 전 API를 한 번씩 불러 요청당 쿼리 수를 수집한다.
//
// 부하가 아니다. 요청은 100회 안팎이고 VU는 1이다. 포화시키면 모든 API가 고르게 느려져
// 어느 API가 비싼지 가려지는데(1라운드 §4.1), 요청당 쿼리 수는 큐 대기에 오염되지 않는다.
//
// 서버에서 request-log.count-sql-statements=true 여야 의미가 있다(구현계획 §5).
// 이 스크립트 자체는 queryCount를 볼 수 없다 — 값은 서버 로그에 남고, 구현계획 §6 절차로 뽑는다.
// 여기서 남기는 것은 requestId → 구간 이름 대응표이고, 그것이 로그와 조인하는 열쇠다.
//
//   ./k6.sh query-count-sweep -e RUN_TAG=qcs-20260828a -e THINK_MIN=0 -e THINK_MAX=0
import exec from 'k6/execution';

import {
  apiGet,
  apiSend,
  dataOf,
  getRequestCount,
  getRequestTrail,
  resetRequestCount,
  setRequestIdPrefix,
} from './lib/api.js';
import { EXCLUDED, aiSearchBody, readOnlySweep, scalePairs } from './lib/api-catalog.js';
import { ORDER_WINDOW, businessHourNow, dishIdFor, storeIdFor } from './lib/config.js';
import {
  buyerPurchase,
  clearLeftoverCartItem,
  openSession,
  resetThinkTotal,
  sellerAdjustStock,
  sellerHandleOrder,
} from './lib/flow.js';

const BUYER_ACCOUNT = Number(__ENV.BUYER_ACCOUNT || 1);
const SELLER_ACCOUNT = Number(__ENV.SELLER_ACCOUNT || 2);
const RUN_TAG = __ENV.RUN_TAG;
const IGNORE_ORDER_WINDOW = __ENV.IGNORE_ORDER_WINDOW === '1';

export const options = {
  vus: 1,
  iterations: 1,
  // 쿼리 수 수집이 목적이라 실패 몇 건으로 실행을 세우지 않는다.
  // 대신 구현계획 §6.4에서 k6가 보낸 요청 수와 로그 행 수를 대조해 누락을 잡는다.
  thresholds: {},
};

export function setup() {
  if (!RUN_TAG) {
    exec.test.abort('RUN_TAG가 필요합니다. 예: -e RUN_TAG=qcs-20260828a');
  }
  const business = businessHourNow();
  if (!business.orderable && !IGNORE_ORDER_WINDOW) {
    exec.test.abort(
      `${business.label}은 주문 가능 구간(${ORDER_WINDOW.fromHour}:00~21:30 KST) 밖입니다. ` +
        '주문 생성이 실패해 order_create/accept/pickup의 쿼리 수가 빠집니다. ' +
        '그래도 진행하려면 -e IGNORE_ORDER_WINDOW=1',
    );
  }
  return {};
}

export default function () {
  setRequestIdPrefix(RUN_TAG);
  resetRequestCount();
  resetThinkTotal();

  const sessions = {
    buyer: openSession(BUYER_ACCOUNT),
    seller: openSession(SELLER_ACCOUNT),
  };
  clearLeftoverCartItem(sessions.buyer);
  clearLeftoverCartItem(sessions.seller);

  // 1단계 — 흐름을 그대로 실행한다. 1라운드가 부하를 준 코드 경로와 같은 코드다.
  const storeId = storeIdFor(SELLER_ACCOUNT);
  const dishId = dishIdFor(SELLER_ACCOUNT);
  const order = buyerPurchase(sessions.buyer, { storeId, dishId });
  const store = sellerAdjustStock(sessions.seller, 1);
  sellerHandleOrder(sessions.seller, store ? store.storeId : storeId);

  const ctx = { storeId, dishId, orderId: order ? order.orderId : null };
  if (!ctx.orderId) {
    console.warn('주문 생성이 실패해 주문 상세 구간을 건너뜁니다.');
  }

  // 픽업코드 조회는 주문이 PICKUP_READY일 때만 200이다. 위 sellerHandleOrder가
  // 수락과 픽업 완료를 한 번에 끝내버려서 그 주문으로는 잴 수 없다(드라이런에서 404 확인).
  // 그래서 주문을 하나 더 만들어 수락까지만 하고 재고, 잰 뒤 픽업으로 정리한다.
  measurePickupCode(sessions, { storeId, dishId });

  // 2단계 — 흐름이 지나가지 않는 읽기 API.
  for (const [step, path, actor] of readOnlySweep(ctx)) {
    if (path.indexOf('null') >= 0) {
      console.warn(`[${step}] 선행 값이 없어 건너뜁니다: ${path}`);
      continue;
    }
    apiGet(step, path, sessions[actor].token);
  }
  apiSend('sweep_ai_search', 'POST', '/ai/search', sessions.buyer.token, aiSearchBody());

  // 정산 상세는 목록이 비어 있으면 부를 수 없다. 건너뛴 사실을 남긴다.
  const settlements = dataOf(
    apiGet('sweep_settlements_list', '/settlements?page=0&size=10', sessions.seller.token),
  );
  const rows = (settlements && settlements.content) || [];
  if (rows.length > 0) {
    apiGet('sweep_settlement_detail', `/settlements/${rows[0].settlementId}`, sessions.seller.token);
  } else {
    console.warn('[sweep_settlement_detail] 정산 내역이 없어 건너뜁니다.');
  }

  // 3단계 — 항목 수만 바꿔 같은 API를 두 번. 기울기로 N+1과 인덱스 문제를 가른다.
  // 두 요청의 requestId는 결과 index 파일에서 순서로 찾는다(작은 쪽이 항상 먼저다).
  for (const [name, smallPath, largePath, actor] of scalePairs(ctx)) {
    apiGet('sweep_scale_small', smallPath, sessions[actor].token);
    apiGet('sweep_scale_large', largePath, sessions[actor].token);
    console.log(`[scale:${name}] small → large 순서로 호출함`);
  }

  console.log(`--- 스윕 완료: 총 HTTP 호출 ${getRequestCount()}회 ---`);
  emitTrail();
}

// 주문을 하나 더 만들어 수락까지만 하고 픽업코드를 잰 뒤, 픽업으로 정리한다.
// 남는 주문을 만들지 않으려고 반드시 픽업까지 끝낸다.
function measurePickupCode(sessions, target) {
  const extra = buyerPurchase(sessions.buyer, target);
  if (!extra || !extra.orderId) {
    console.warn('[sweep_order_pickup_code] 추가 주문 생성 실패로 건너뜁니다.');
    return;
  }

  const accepted = dataOf(
    apiSend('sweep_pickup_code_accept', 'POST', `/orders/${extra.orderId}/accept`, sessions.seller.token, null),
  );
  if (!accepted) {
    console.warn(`[sweep_order_pickup_code] 주문 ${extra.orderId} 수락 실패로 건너뜁니다.`);
    return;
  }

  apiGet('sweep_order_pickup_code', `/orders/${extra.orderId}/pickupCode`, sessions.buyer.token);

  // 정리 — PICKUP_READY로 남겨두지 않는다.
  apiSend('sweep_pickup_code_cleanup', 'PATCH', `/orders/${extra.orderId}/pickup`, sessions.seller.token, {
    status: 'PICKED_UP',
  });
}

// handleSummary는 VU와 다른 컨텍스트에서 돌아 모듈 상태(requestTrail)를 보지 못한다.
// 드라이런에서 index.tsv가 헤더만 남는 것으로 확인했다. 그래서 VU 안에서 직접 찍고,
// 로그에서 뽑아 쓴다(구현계획 §6도 어차피 로그 기반 파이프라인이다).
//
// 구분자가 탭이면 k6 기본 logfmt가 \t로 이스케이프해 버려 그대로 못 쓴다(드라이런에서 확인).
// 파이프는 이스케이프되지 않아 --log-format 무엇이든 살아남는다.
function emitTrail() {
  const trail = getRequestTrail();
  console.log(`QCS_TRAIL_BEGIN ${RUN_TAG} rows=${trail.length}`);
  for (const r of trail) {
    console.log(`QCS_TRAIL|${r.requestId}|${r.step}|${r.status}|${r.durationMs}|${r.url}`);
  }
  console.log('QCS_TRAIL_END');
}

export function handleSummary() {
  return {
    stdout:
      `요청 번호 대응표는 k6 콘솔 로그의 QCS_TRAIL 줄에 있습니다.\n` +
      `  ./k6.sh query-count-sweep ... --log-format=raw 2>&1 | tee results/${RUN_TAG}-k6.log\n` +
      `  grep '^QCS_TRAIL|' results/${RUN_TAG}-k6.log | cut -d'|' -f2- > results/${RUN_TAG}-index.psv\n`,
    // 미측정으로 남긴 API와 그 이유. init 컨텍스트의 상수라 여기서 쓸 수 있다.
    [`/results/${RUN_TAG}-excluded.json`]: JSON.stringify(EXCLUDED, null, 2),
  };
}
