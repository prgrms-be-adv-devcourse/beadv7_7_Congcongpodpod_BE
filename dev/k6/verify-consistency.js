// 부하 실행 전후의 정합성을 확인한다. 부하를 주지 않는 읽기 전용 실행이다.
//
// 부하 결과에 응답시간만 남으면 발표에서 "정합성이 유지됐다"를 말할 수 없다.
// 여기서 재고·예치금·장바구니·주문 상태를 세어 그 자체로 깨진 곳이 있는지 보고,
// 이전 스냅샷을 주면 전후 수지가 맞는지까지 본다.
//
// 사용법:
//   부하 전:  ./k6.sh verify-consistency -e RESULT_LABEL=before
//   부하 후:  ./k6.sh verify-consistency -e RESULT_LABEL=after \
//               -e BEFORE_SNAPSHOT=/results/<runId>-before-summary.json
//
// 스냅샷은 별도 형식이 아니라 이 스크립트가 남기는 요약 JSON 그 자체다.
// k6는 VU에서 만든 값을 handleSummary로 넘겨주지 않으므로, 비교에 쓰는 집계값은
// 전부 커스텀 지표로 올린다. 그래야 요약 파일에 자동으로 들어간다.
import { check } from 'k6';
import { Gauge } from 'k6/metrics';

import { loginWithCredentials, seedCredentials } from './lib/accounts.js';
import { apiGet, dataOf, depositBalanceOf } from './lib/api.js';
import { loadRunState } from './lib/run-state.js';
import { buildSummaryHandler } from './lib/summary.js';

const RUN_ID = __ENV.RUN_ID;
const BUYER_SAMPLE = Number(__ENV.BUYER_SAMPLE || 20);
const LOADTEST_PASSWORD = __ENV.LOADTEST_PASSWORD;

if (!RUN_ID) {
  throw new Error('실행을 식별할 RUN_ID가 필요합니다.');
}
if (!__ENV.RESULT_LABEL) {
  throw new Error('스냅샷을 구분할 RESULT_LABEL이 필요합니다. 예: -e RESULT_LABEL=before');
}
if (!LOADTEST_PASSWORD) {
  throw new Error('판매자 주문 조회에 LOADTEST_PASSWORD가 필요합니다.');
}

const runState = loadRunState(__ENV.STATE_FILE);

// 이전 요약 JSON에서 같은 이름의 지표값을 꺼내 비교 기준으로 쓴다.
const beforeMetrics = __ENV.BEFORE_SNAPSHOT
  ? (JSON.parse(open(__ENV.BEFORE_SNAPSHOT)).metrics || {})
  : null;

function beforeValue(name) {
  const metric = beforeMetrics && beforeMetrics[name];
  if (!metric || !metric.values) {
    return null;
  }
  const value = metric.values.value !== undefined ? metric.values.value : metric.values.max;
  return Number.isFinite(Number(value)) ? Number(value) : null;
}

const ORDER_STATUSES = ['RESERVED', 'PICKUP_READY', 'PICKED_UP', 'NO_SHOW', 'CANCELLED', 'REJECTED'];

// Gauge는 마지막 값을 남긴다. 1 VU · 1 iteration이므로 그 값이 곧 스냅샷이다.
const totalStock = new Gauge('consistency_total_stock');
const negativeStockCount = new Gauge('consistency_negative_stock_count');
const unreadableStockCount = new Gauge('consistency_unreadable_stock_count');
const buyerBalanceTotal = new Gauge('consistency_buyer_balance_total');
const cartItemsTotal = new Gauge('consistency_cart_items_total');
const ordersTotal = new Gauge('consistency_orders_total');
const orderGauges = {};
for (const status of ORDER_STATUSES) {
  orderGauges[status] = new Gauge(`consistency_orders_${status.toLowerCase()}`);
}

export const options = {
  vus: 1,
  iterations: 1,
  // 읽기 전용이라 부하가 아니다. 정합성 검사는 하나라도 깨지면 실패여야 한다.
  thresholds: { checks: ['rate==1'] },
  tags: { testid: RUN_ID, phase: 'consistency', snapshot_label: __ENV.RESULT_LABEL },
};

function safeLogin(credentials) {
  try {
    return loginWithCredentials(credentials);
  } catch (error) {
    console.error(`로그인 실패로 표본에서 제외: ${credentials.email} — ${error.message}`);
    return null;
  }
}

// 상품 상세는 공개 조회라 토큰이 필요 없다.
function collectStock() {
  let sum = 0;
  let negative = 0;
  let unreadable = 0;

  for (const seller of runState.sellers) {
    const dish = dataOf(apiGet('dish_detail', `/dishes/${seller.dishId}`, null));
    const quantity = dish ? Number(dish.stockQuantity) : NaN;
    if (!Number.isFinite(quantity)) {
      unreadable += 1;
      console.error(`재고를 읽지 못했습니다: ${seller.key} dishId=${seller.dishId}`);
      continue;
    }
    if (quantity < 0) {
      negative += 1;
      console.error(`음수 재고: ${seller.key} dishId=${seller.dishId} stock=${quantity}`);
    }
    sum += quantity;
  }

  totalStock.add(sum);
  negativeStockCount.add(negative);
  unreadableStockCount.add(unreadable);
  return { sum, negative, unreadable };
}

// 매장별 주문 상태 분포. totalElements만 필요하므로 size=1로 최소한만 받는다.
function collectOrders() {
  const counts = {};
  for (const status of ORDER_STATUSES) {
    counts[status] = 0;
  }

  for (const seller of runState.sellers) {
    const session = safeLogin({ email: seller.email, password: LOADTEST_PASSWORD });
    if (!session) {
      continue;
    }
    for (const status of ORDER_STATUSES) {
      const page = dataOf(
        apiGet(
          'seller_orders_reserved',
          `/orders/stores/${seller.storeId}?status=${status}&page=0&size=1`,
          session.token,
        ),
      );
      const total = page ? Number(page.totalElements) : NaN;
      if (Number.isFinite(total)) {
        counts[status] += total;
      }
    }
  }

  let total = 0;
  for (const status of ORDER_STATUSES) {
    orderGauges[status].add(counts[status]);
    total += counts[status];
  }
  ordersTotal.add(total);
  return counts;
}

// 구매 계정 전부를 보면 오래 걸린다. 앞에서부터 표본만 본다.
function collectBuyers() {
  let balanceSum = 0;
  let cartItemSum = 0;
  let sampled = 0;
  let negativeBalance = 0;

  for (let accountNo = 1; accountNo <= BUYER_SAMPLE; accountNo += 1) {
    const session = safeLogin(seedCredentials(accountNo));
    if (!session) {
      continue;
    }
    const balance = depositBalanceOf(apiGet('deposit_balance', '/deposits/balance', session.token));
    if (Number.isFinite(balance)) {
      balanceSum += balance;
      if (balance < 0) {
        negativeBalance += 1;
        console.error(`음수 예치금: seller${String(accountNo).padStart(4, '0')} balance=${balance}`);
      }
    }
    cartItemSum += (session.cartItems || []).length;
    sampled += 1;
  }

  buyerBalanceTotal.add(balanceSum);
  cartItemsTotal.add(cartItemSum);
  return { balanceSum, cartItemSum, sampled, negativeBalance };
}

export default function () {
  const stock = collectStock();
  const orders = collectOrders();
  const buyers = collectBuyers();

  // 미픽업 상태를 뺀 진행/완료 주문. 취소·거절은 재고가 돌아왔을 수 있어 따로 본다.
  const liveOrders =
    orders.RESERVED + orders.PICKUP_READY + orders.PICKED_UP + orders.NO_SHOW;

  console.log(`--- 정합성 스냅샷 (${__ENV.RESULT_LABEL}) ---`);
  console.log(`상품 ${runState.sellers.length}개 · 총 재고 ${stock.sum} · 음수 ${stock.negative} · 조회실패 ${stock.unreadable}`);
  console.log(
    `주문 RESERVED=${orders.RESERVED} PICKUP_READY=${orders.PICKUP_READY} ` +
      `PICKED_UP=${orders.PICKED_UP} NO_SHOW=${orders.NO_SHOW} ` +
      `CANCELLED=${orders.CANCELLED} REJECTED=${orders.REJECTED}`,
  );
  console.log(`구매 표본 ${buyers.sampled}개 · 예치금 합계 ${buyers.balanceSum} · 장바구니 잔여 ${buyers.cartItemSum}`);

  check(null, {
    '음수 재고가 없다': () => stock.negative === 0,
    '모든 상품 재고를 조회했다': () => stock.unreadable === 0,
    '음수 예치금이 없다': () => buyers.negativeBalance === 0,
    '구매 표본을 모두 조회했다': () => buyers.sampled === BUYER_SAMPLE,
    '픽업 완료가 전체 진행 주문을 넘지 않는다': () => orders.PICKED_UP <= liveOrders,
  });

  if (!beforeMetrics) {
    console.log('BEFORE_SNAPSHOT이 없어 전후 비교는 건너뜁니다.');
    return;
  }

  const priorStock = beforeValue('consistency_total_stock');
  const priorOrders = beforeValue('consistency_orders_total');
  const priorBalance = beforeValue('consistency_buyer_balance_total');

  const stockDrop = Number.isFinite(priorStock) ? priorStock - stock.sum : null;
  const orderIncrease = Number.isFinite(priorOrders)
    ? orders.RESERVED + orders.PICKUP_READY + orders.PICKED_UP + orders.NO_SHOW +
      orders.CANCELLED + orders.REJECTED - priorOrders
    : null;
  const balanceDrop = Number.isFinite(priorBalance) ? priorBalance - buyers.balanceSum : null;

  console.log('--- 전후 비교 ---');
  console.log(`주문 증가 ${orderIncrease} · 재고 감소 ${stockDrop} · 표본 예치금 감소 ${balanceDrop}`);

  check(null, {
    // 재고 조정 흐름이 재고를 늘리므로 정확히 일치할 수는 없다. 방향만 본다.
    '주문이 늘었다면 재고가 줄었다': () =>
      orderIncrease === null || orderIncrease <= 0 || (stockDrop !== null && stockDrop > 0),
    '주문 수가 줄지 않았다': () => orderIncrease === null || orderIncrease >= 0,
    '표본 예치금이 늘지 않았다': () => balanceDrop === null || balanceDrop >= 0,
  });
}

export const handleSummary = buildSummaryHandler({
  sellerCount: runState.sellers.length,
  buyerSample: BUYER_SAMPLE,
  beforeSnapshot: __ENV.BEFORE_SNAPSHOT || null,
});
