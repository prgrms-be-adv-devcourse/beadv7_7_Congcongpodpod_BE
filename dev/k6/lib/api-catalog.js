// 흐름(lib/flow.js)이 지나가지 않는 API 목록. 호출 로직은 갖지 않고 "무엇을 부를지"만 데이터로 들고 있다.
// 흐름이 이미 부르는 API는 여기 두지 않는다 — 두 번 부르면 산출물 표에 중복 행이 생긴다.
//
// 경로는 2026-08-28에 컨트롤러를 직접 읽어 확인했다.
import { PAGE, SEED } from './config.js';

// 부르지 않기로 한 API와 그 이유. 산출물 표에 "미측정"으로 함께 싣는다.
// 이유를 코드에 남겨야 나중에 "왜 이 API는 표에 없나"를 다시 조사하지 않는다.
export const EXCLUDED = [
  { api: 'POST /auth/signup', reason: '회원이 생겨 시드 집합이 오염된다' },
  { api: 'PATCH /auth/withdraw', reason: '회원 탈퇴. 되돌릴 수 없다' },
  { api: 'POST /auth/kakao', reason: '외부 OAuth 인가 코드 필요' },
  { api: 'POST /payments, POST /payments/approve', reason: '실제 결제' },
  { api: 'POST /ai (classify)', reason: 'OpenAI 과금' },
  { api: 'POST /ai/index/sync, POST /ai/index/init', reason: '검색 색인 직접 조작' },
  { api: 'POST /settlements/jobs', reason: '월 정산 배치. 데이터 대량 생성' },
  { api: 'POST /stores, POST /dishes', reason: '데이터 규모 기준선이 흔들린다' },
  { api: 'PATCH /stores/{id}/delete, PATCH /dishes/{id}', reason: '삭제. 되돌릴 수 없다' },
  { api: 'GET/POST/PUT /stores/{id}/payoutAccount', reason: '암호화 저장되는 금융 정보' },
  { api: 'PATCH /notifications/read-all', reason: '대량 상태 변경. 되돌릴 수 없다' },
  { api: 'GET /notifications/stream (SSE)', reason: 'SSE. 끊길 때까지 완료 로그가 없다' },
  { api: 'POST /dishes/images/presigned-url', reason: '업로드용 발급. 쓰기 성격' },
  {
    api: 'GET /dishes/{id}/image/presigned-url',
    reason:
      '시드 상품은 thumbnail_url이 비어 있어 400 IMG008로 끝난다(2026-08-28 드라이런 확인). '
      + '재려면 이미지를 먼저 업로드해야 하는데 그것이 데이터를 바꾼다',
  },
  { api: '/internal/v1/**', reason: '게이트웨이 라우트 없음(anyExchange().denyAll())' },
];

// 읽기 전용 스윕. ctx = { storeId, dishId, orderId }
// actor는 어느 세션의 토큰으로 부를지를 가리킨다('buyer' | 'seller').
export function readOnlySweep(ctx) {
  return [
    ['sweep_notifications', '/notifications?page=0&size=10', 'buyer'],
    ['sweep_notifications_unread', '/notifications/unread-count', 'buyer'],
    ['sweep_points_balance', '/points/balance', 'buyer'],
    ['sweep_points_history', '/points/history?page=0&size=10', 'buyer'],
    ['sweep_deposit_history', '/deposits/history?page=0&size=10', 'buyer'],
    ['sweep_level_info', '/levels/info', 'buyer'],
    ['sweep_favorites_list', '/favorites', 'buyer'],
    ['sweep_favorite_status', `/favorites/${ctx.storeId}`, 'buyer'],
    ['sweep_order_detail', `/orders/${ctx.orderId}`, 'buyer'],
    ['sweep_seller_my_dish', `/stores/${ctx.storeId}/dish`, 'seller'],
    ['sweep_seller_my_dishes', `/stores/${ctx.storeId}/dishes`, 'seller'],
    [
      'sweep_ai_nearby',
      `/ai/stores/nearby?latitude=${SEED.latitude}&longitude=${SEED.longitude}&radiusKm=${PAGE.nearbyRadiusKm}`,
      'buyer',
    ],
    // 한글 질의는 인코딩이 필요하고, 지오코딩이 실제로 찾을 수 있는 주소여야 한다.
    // '강남대로'만으로는 404 GEO001이 떨어진다 — 번지까지 준다(2026-08-28 드라이런에서 200 확인).
    ['sweep_geocode', `/locations/geocode?query=${encodeURIComponent('강남대로 396')}`, 'buyer'],
  ];
}

// POST /ai/search의 본문. StoreSearchRequest는 query·latitude·longitude가 필수다.
export function aiSearchBody() {
  return {
    query: '치킨',
    latitude: SEED.latitude,
    longitude: SEED.longitude,
    radiusKm: PAGE.nearbyRadiusKm,
  };
}

// 항목 수에 비례해 쿼리가 느는지 보기 위한 짝. [이름, 작은 쪽, 큰 쪽, actor]
// 같은 API를 크기만 바꿔 두 번 부른다. 쿼리 수가 항목 수를 따라 늘면 N+1이고,
// 그대로인데 시간만 늘면 인덱스 문제다 — 이 구분이 이 짝의 존재 이유다.
export function scalePairs(ctx) {
  const near = `latitude=${SEED.latitude}&longitude=${SEED.longitude}&radiusKm=${PAGE.nearbyRadiusKm}`;
  return [
    ['stores_nearby', `/stores/nearby?${near}&page=0&size=5`, `/stores/nearby?${near}&page=0&size=30`, 'buyer'],
    ['my_orders', '/orders?page=0&size=5', '/orders?page=0&size=50', 'buyer'],
    [
      'store_orders',
      `/orders/stores/${ctx.storeId}?page=0&size=5`,
      `/orders/stores/${ctx.storeId}?page=0&size=50`,
      'seller',
    ],
    ['notifications', '/notifications?page=0&size=5', '/notifications?page=0&size=50', 'buyer'],
    ['deposit_history', '/deposits/history?page=0&size=5', '/deposits/history?page=0&size=50', 'buyer'],
    ['points_history', '/points/history?page=0&size=5', '/points/history?page=0&size=50', 'buyer'],
  ];
}
