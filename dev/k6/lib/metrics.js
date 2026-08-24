// 설계 문서 9절 "실행 후 반드시 기록할 값"을 k6 커스텀 지표로 옮긴 것이다.
// k6는 init 컨텍스트에서만 지표를 만들 수 있어 구간 이름을 여기에 전부 선언한다.
import { Trend, Counter } from 'k6/metrics';

// 화면 흐름의 직렬 응답 대기 구간. 병렬(batch) 묶음은 한 구간으로 센다.
export const STEPS = [
  'auth_login',
  'auth_refresh',
  'member_me',
  'cart_get',
  'cart_cleanup',
  'store_nearby',
  'store_detail_batch',
  'dish_detail',
  'dish_detail_store',
  'cart_add',
  'deposit_balance',
  'cart_revalidate',
  'order_create',
  'cart_after_order',
  'order_list',
  'order_list_stores_batch',
  'order_pickup_codes_batch',
  'seller_stores_for_dish',
  'seller_dish_list',
  'dish_stock_adjust',
  'seller_stores_for_order',
  'seller_orders_reserved',
  'order_accept',
  'seller_orders_pickup_ready',
  'order_pickup',
  'lifecycle_signup',
  'lifecycle_store_create',
  'lifecycle_dish_create',
];

const trends = {};
for (const step of STEPS) {
  trends[step] = new Trend(`step_${step}`, true);
}

// 구간 이름을 오타로 적으면 지표가 조용히 사라지므로 여기서 즉시 실패시킨다.
export function stepTrend(step) {
  const trend = trends[step];
  if (!trend) {
    throw new Error(`metrics.js STEPS에 없는 구간 이름입니다: ${step}`);
  }
  return trend;
}

// 세션 단위 실측값 — 설계 문서 6.3절의 T(L, D)를 대체할 값
export const sessionDuration = new Trend('flow_session_duration', true);
export const thinkTimeTotal = new Trend('flow_think_time_total', true);
export const iterationRequests = new Trend('flow_iteration_requests');

// 주문 목록 호출 수의 반복별 감소 — 설계 문서 5절 표를 실측으로 바꾸는 값
export const orderListCalls = new Trend('flow_order_list_calls');
export const orderListStoreFanout = new Trend('flow_order_list_store_fanout');
export const orderListPickupCodeCalls = new Trend('flow_order_list_pickup_code_calls');

// 새 주문 발견 — 설계 문서 6.2절
export const sellerOrderRetries = new Trend('flow_seller_order_retries');
export const sellerOrderDiscovery = new Trend('flow_seller_order_discovery', true);

// 상태 전이 성공 수
export const ordersCreated = new Counter('flow_orders_created');
export const ordersAccepted = new Counter('flow_orders_accepted');
export const ordersPickedUp = new Counter('flow_orders_picked_up');

// 건너뛴 경우 — 실패와 구분해서 센다
export const sellerOrderNotFound = new Counter('flow_seller_order_not_found');
export const dishStockAdjustSkipped = new Counter('flow_dish_stock_adjust_skipped');

// 부하가 데이터를 정상적으로 소진해서 생긴 업무 결과. 서버 장애·계약 오류와 분리한다.
export const expectedBusinessOutcomes = new Counter('flow_expected_business_outcomes');

// 재고 조정 의도량. 최종 재고와 대조해 덮어쓰기 여부를 판정한다 (설계 문서 8.3절).
export const dishStockDeltaUp = new Counter('flow_dish_stock_delta_up');
export const dishStockDeltaDown = new Counter('flow_dish_stock_delta_down');
