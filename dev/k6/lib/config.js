// 시나리오 전체가 공유하는 환경변수와 시드 상수를 모은다.
// 시드 값은 backend/services/*/src/main/resources/db/seed/R__demo_seed.sql 실측 기준이다.

const trimTrailingSlash = (value) => (value || '').replace(/\/$/, '');

export const BASE_URL = trimTrailingSlash(__ENV.BASE_URL);

if (!BASE_URL) {
  throw new Error('BASE_URL 환경변수가 필요합니다.');
}

export const API = `${BASE_URL}/api/v1`;

export const REQUEST_TIMEOUT = __ENV.REQUEST_TIMEOUT || '10s';

export const SEED = {
  // dev/README.md:27은 3자리(seller001)로 문서화돼 있지만 실제 시드는 4자리다.
  // member-service 커밋 a7f6f0ca(2026-08-27)에서 회원 1~1000 전체를 4자리(seller0001)로
  // 바꿨다(100번/1000번 회원 고유키 충돌 해결). accountEmail()도 이를 따른다.
  password: __ENV.SEED_PASSWORD || 'LastDish!2026',
  emailDomain: 'seed.lastdish.kr',
  accountCount: Number(__ENV.SEED_ACCOUNT_COUNT || 300),
  // 회원 1~150만 예치금 10,000,000. 151~300은 0이라 주문이 실패한다.
  fundedAccountCount: 150,
  // 매장 시드 영업시간. Store.isOpenAt / calculatePickupDeadline이 주문 가능 시각을 09:00~21:30으로 제한한다.
  storeOpenTime: '09:00',
  storeCloseTime: '22:00',
  // 시드 주문은 1차 1~300,000 + 정산 테스트 300,001~750,000이고 orders_id_seq도 750000이다.
  // 실행 중 생성되는 주문은 750,001부터라 이 값보다 큰 주문만 "새 주문"이다.
  newOrderIdMin: 750000,
  // 남부터미널역 일대 — 시드 매장이 밀집한 좌표
  latitude: 37.4851,
  longitude: 127.0158,
};

// OrderFacade가 Asia/Seoul로 영업 여부와 픽업 마감을 판단한다. k6 컨테이너는 UTC라 직접 환산한다.
export const BUSINESS_ZONE_OFFSET_HOURS = 9;

// 주문 생성이 성공할 수 있는 KST 구간. 09:00은 매장 오픈, 21:30은 시드 상품의 픽업 마감이다.
export const ORDER_WINDOW = { fromHour: 9, toHour: 21.5 };

// 지금이 KST 기준 몇 시인지(소수 시간)와 주문 가능 구간 안인지 돌려준다.
export function businessHourNow() {
  const kst = new Date(Date.now() + BUSINESS_ZONE_OFFSET_HOURS * 3600 * 1000);
  const hour = kst.getUTCHours() + kst.getUTCMinutes() / 60;
  return {
    hour,
    label: `${String(kst.getUTCHours()).padStart(2, '0')}:${String(kst.getUTCMinutes()).padStart(2, '0')} KST`,
    orderable: hour >= ORDER_WINDOW.fromHour && hour <= ORDER_WINDOW.toHour,
  };
}

// 사용자 행동 12곳에 넣는 대기시간(초). 설계 문서 6.1절.
export const THINK_MIN = Number(__ENV.THINK_MIN || 1);
export const THINK_MAX = Number(__ENV.THINK_MAX || 3);

// 매장 사용자가 새 주문을 못 찾았을 때의 추가 조회. 설계 문서 6.2절.
export const SELLER_ORDER_RETRY = Number(__ENV.SELLER_ORDER_RETRY || 2);
export const SELLER_ORDER_RETRY_WAIT = Number(__ENV.SELLER_ORDER_RETRY_WAIT || 1);

// RN 화면이 실제로 쓰는 조회 파라미터 (frontend/react-native/src/lib 실측)
export const PAGE = {
  nearbyRadiusKm: 5,
  nearbySize: 30,
  myOrdersSize: 50,
};

// 시드 계정 번호 → 로그인 이메일. 회원 1~1000 전체가 4자리 형식이다(SEED 주석 참고).
export function accountEmail(accountNo) {
  return `seller${String(accountNo).padStart(4, '0')}@${SEED.emailDomain}`;
}

// 시드는 회원 = 매장 = 상품이 모두 같은 번호다.
export function storeIdFor(accountNo) {
  return accountNo;
}

export function dishIdFor(accountNo) {
  return accountNo;
}
