/**
 * 티켓팅형 재고 동시성 부하테스트.
 *
 * 사전 준비:
 *   1. 대상 dish 재고를 원하는 수량으로 낮춰둔다.
 *   2. dev/k6/accounts.csv (email,password)에 VUS 이상의 테스트 계정을 준비한다.
 *      각 계정은 대상 dish를 장바구니에 담아두고, 예치금 잔액을 충분히 채워둔다.
 *      accounts.csv는 커밋하지 않는다 (accounts.csv.example 참고).
 *
 * 실행:
 *   ./k6.sh order-stock-race -e DISH_ID=123 -e VUS=30
 *
 * 결과 판정은 k6 기본 지표가 아니라 아래 커스텀 카운터로 한다.
 *   - order_success      주문 성공(200)
 *   - order_out_of_stock 품절 경쟁 탈락(409 + D001 또는 D003) — 정상적인 탈락
 *   - order_unexpected   그 외 전부 — 1건이라도 있으면 결함 신호
 *
 * 주의: k6는 409를 http_req_failed로 집계한다. 재고 5에 VU 30이면
 * http_req_failed가 80%대로 찍히지만 이는 정상이다. 위 카운터로 판단할 것.
 */
import http from 'k6/http';
import { check } from 'k6';
import { SharedArray } from 'k6/data';
import { Counter } from 'k6/metrics';

const baseUrl = (__ENV.BASE_URL || '').replace(/\/$/, '');
const dishId = __ENV.DISH_ID;
const vus = parseInt(__ENV.VUS || '0', 10);
const accountsFile = __ENV.ACCOUNTS_FILE || 'accounts.csv';

if (!baseUrl) {
  throw new Error('BASE_URL 환경변수가 필요합니다.');
}
if (!dishId) {
  throw new Error('DISH_ID 환경변수가 필요합니다. 예: -e DISH_ID=123');
}
if (!vus || vus < 1) {
  throw new Error('VUS 환경변수가 필요합니다 (1 이상). 예: -e VUS=30');
}

function parseAccountsCsv(text) {
  const lines = text
    .trim()
    .split('\n')
    .map((line) => line.trim())
    .filter((line) => line.length > 0);
  const [header, ...rows] = lines;
  const columns = header.split(',').map((column) => column.trim());
  const emailIndex = columns.indexOf('email');
  const passwordIndex = columns.indexOf('password');
  if (emailIndex === -1 || passwordIndex === -1) {
    throw new Error(`${accountsFile}에 email, password 컬럼이 필요합니다.`);
  }
  return rows.map((row) => {
    const cells = row.split(',');
    return { email: cells[emailIndex].trim(), password: cells[passwordIndex].trim() };
  });
}

const accounts = new SharedArray('accounts', () => parseAccountsCsv(open(accountsFile)));

if (accounts.length < vus) {
  throw new Error(`계정이 부족합니다: accounts=${accounts.length}, VUS=${vus}`);
}

export const options = {
  scenarios: {
    order_stock_race: {
      executor: 'per-vu-iterations',
      vus,
      iterations: 1,
      maxDuration: '30s',
    },
  },
};

const orderSuccess = new Counter('order_success');
const orderOutOfStock = new Counter('order_out_of_stock');
const orderUnexpected = new Counter('order_unexpected');

// 타임아웃·네트워크 오류일 때 k6는 status 0에 body null을 준다. 로그 경로가 거기서 죽지 않게 감싼다.
function bodyPreview(response) {
  return response.body ? response.body.slice(0, 500) : '(응답 본문 없음)';
}

// 에러 코드를 못 읽어도 분류가 멈추면 안 된다. 파싱 실패는 정상 품절 응답이 아닌 것으로 취급한다.
function errorCodeOf(response) {
  try {
    return response.json('error.code');
  } catch (_) {
    return null;
  }
}

// 마지막 재고를 차감하면 상품 상태도 SOLD_OUT으로 바뀐다. 이후 요청은 재고 비교 전에
// 판매 상태 검증에서 D001을 받을 수 있으므로 D001과 D003을 모두 정상적인 경쟁 탈락으로 본다.
function isExpectedStockRejection(response) {
  if (response.status !== 409) {
    return false;
  }

  const errorCode = errorCodeOf(response);
  return errorCode === 'D001' || errorCode === 'D003';
}

function login(email, password) {
  const response = http.post(
    `${baseUrl}/api/v1/auth/login`,
    JSON.stringify({ email, password }),
    { headers: { 'Content-Type': 'application/json' }, tags: { name: 'Login' } },
  );
  if (response.status !== 200) {
    throw new Error(
      `로그인 실패: email=${email}, status=${response.status}, body=${bodyPreview(response)}`,
    );
  }
  return response.json('data.accessToken');
}

function findCartItem(email, accessToken) {
  const response = http.get(`${baseUrl}/api/v1/carts/members`, {
    headers: { Authorization: `Bearer ${accessToken}` },
    tags: { name: 'Get cart' },
  });
  if (response.status !== 200) {
    throw new Error(
      `장바구니 조회 실패: email=${email}, status=${response.status}, body=${bodyPreview(response)}`,
    );
  }
  const items = response.json('data.items');
  const item = items.find((candidate) => String(candidate.dishId) === String(dishId));
  if (!item) {
    throw new Error(`대상 dish(${dishId})가 담긴 장바구니 항목을 찾지 못했습니다: email=${email}`);
  }
  return { cartItemId: item.cartItemId, dishPriceVersion: item.lastAppliedDishPriceVersion };
}

// 대상 dish의 현재 재고. 조회에 실패하면 null을 돌려 호출부가 판정 불가를 구분하게 한다.
function fetchStockQuantity() {
  const response = http.get(`${baseUrl}/api/v1/dishes/${dishId}`, {
    tags: { name: 'Get dish stock' },
  });
  if (response.status !== 200) {
    console.error(`dish 조회 실패: status=${response.status}, body=${bodyPreview(response)}`);
    return null;
  }
  return response.json('data.stockQuantity');
}

export function setup() {
  const selected = accounts.slice(0, vus);
  const preparedAccounts = selected.map(({ email, password }) => {
    const accessToken = login(email, password);
    const { cartItemId, dishPriceVersion } = findCartItem(email, accessToken);
    return { accessToken, cartItemId, dishPriceVersion };
  });

  // 최초 재고를 측정 전에 잡아둬야 teardown에서 "성공 건수만큼 줄었는가"를 판정할 수 있다.
  const initialStock = fetchStockQuantity();
  console.log(`[order-stock-race] 최초 재고: ${initialStock}, 동시 주문 시도: ${vus}`);

  return { accounts: preparedAccounts, initialStock };
}

export default function (setupData) {
  const account = setupData.accounts[(__VU - 1) % setupData.accounts.length];

  const response = http.post(
    `${baseUrl}/api/v1/orders/cartItems/${account.cartItemId}`,
    JSON.stringify({ dishPriceVersion: account.dishPriceVersion }),
    {
      headers: {
        Authorization: `Bearer ${account.accessToken}`,
        'Content-Type': 'application/json',
      },
      tags: { name: 'Create order' },
    },
  );

  if (response.status === 200) {
    orderSuccess.add(1);
    check(response, { '주문 성공 응답이 success:true다': (r) => r.json('success') === true });
  } else if (isExpectedStockRejection(response)) {
    orderOutOfStock.add(1);
  } else {
    orderUnexpected.add(1);
    console.error(
      `예상치 못한 주문 응답: status=${response.status}, body=${bodyPreview(response)}`,
    );
  }
}

export function teardown(setupData) {
  const finalStock = fetchStockQuantity();
  if (finalStock === null || setupData.initialStock === null) {
    console.error('[order-stock-race] 재고를 확인하지 못해 오버셀 여부를 판정할 수 없습니다.');
    return;
  }

  const decreased = setupData.initialStock - finalStock;
  console.log(
    `[order-stock-race] 재고 ${setupData.initialStock} -> ${finalStock} (감소 ${decreased})`,
  );
  console.log(
    '[order-stock-race] 판정: 위 감소량과 order_success 카운터가 같아야 한다. ' +
      'order_success가 더 크면 오버셀, 더 작으면 재고가 덜 차감된 것이다.',
  );
}
