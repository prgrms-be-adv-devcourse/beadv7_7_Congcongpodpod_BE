// 운영에 PATCH /api/v1/dishes/{dishId}/stock (PR #359)이 배포됐는지 확인한다.
//
// DishService.adjustStock은 quantityDelta가 0이면 저장소를 건드리기 전에 예외를 던진다.
//   400 D008 (INVALID_STOCK_DELTA) → PATCH 재고 조정 배포됨
//   404 / 405                       → 미배포. 재고 흐름이 포함된 부하 실행 중단
//
// 재고를 바꾸지 않는다.
import http from 'k6/http';
import { API, SEED, accountEmail, dishIdFor } from './lib/config.js';

const ACCOUNT = Number(__ENV.SELLER_ACCOUNT || 2);

export const options = { vus: 1, iterations: 1, thresholds: {} };

export default function () {
  const headers = { 'Content-Type': 'application/json' };

  const loginResponse = http.post(
    `${API}/auth/login`,
    JSON.stringify({ email: accountEmail(ACCOUNT), password: SEED.password }),
    { headers, tags: { name: 'probe_login' } },
  );
  const token = JSON.parse(loginResponse.body).data.accessToken;

  const dishId = dishIdFor(ACCOUNT);
  const response = http.patch(
    `${API}/dishes/${dishId}/stock`,
    JSON.stringify({ quantityDelta: 0 }),
    { headers: Object.assign({ Authorization: `Bearer ${token}` }, headers), tags: { name: 'probe_stock' } },
  );

  let code = null;
  try {
    const parsed = JSON.parse(response.body);
    code = parsed && parsed.error ? parsed.error.code : null;
  } catch (_) {
    code = null;
  }

  console.log('--- PATCH /dishes/{dishId}/stock 확인 ---');
  console.log(`status=${response.status} code=${code}`);
  console.log(`body=${String(response.body).slice(0, 300)}`);
  if (code === 'D008') {
    console.log('판정: 배포됨 → PATCH /dishes/{dishId}/stock을 사용할 수 있습니다.');
  } else if (response.status === 404 || response.status === 405) {
    console.log('판정: 미배포 → 재고 흐름이 포함된 부하 실행을 중단하세요.');
  } else {
    console.log('판정: 불명확 — 위 body를 보고 판단이 필요합니다.');
  }
}
