// X-Request-Id 부여와 흔적 기록이 실제로 도는지 확인한다.
// 서버 로그의 queryCount와 조인하려면 이 대응표가 정확해야 하므로, 스윕 전에 이것부터 통과시킨다.
//
//   ./k6.sh tests/request-id-trail.test
import { check } from 'k6';

import { apiGet, getRequestTrail, setRequestIdPrefix } from '../lib/api.js';

export const options = {
  vus: 1,
  iterations: 1,
  thresholds: {
    checks: ['rate==1'],
  },
};

export default function () {
  // 접두사를 안 주면 헤더를 붙이지 않고 흔적도 남기지 않는다(기존 시나리오 동작 보존).
  setRequestIdPrefix(null);
  apiGet('store_nearby', '/stores/nearby?latitude=37.4851&longitude=127.0158&radiusKm=5&page=0&size=1', null);
  check(getRequestTrail(), {
    '접두사가 없으면 흔적을 남기지 않는다': (trail) => trail.length === 0,
  });

  // 접두사를 주면 요청마다 번호가 붙고, 서버가 그 값을 그대로 돌려준다.
  const tag = 'qcs-selftest';
  setRequestIdPrefix(tag);
  apiGet('store_nearby', '/stores/nearby?latitude=37.4851&longitude=127.0158&radiusKm=5&page=0&size=1', null);
  apiGet('dish_detail', '/dishes/1', null);

  const trail = getRequestTrail();
  check(trail, {
    '요청 2건의 흔적이 남는다': (t) => t.length === 2,
    '요청 번호가 접두사-일련번호 형식이다': (t) => t[0].requestId === `${tag}-0001`,
    '두 번째 요청은 번호가 하나 증가한다': (t) => t[1].requestId === `${tag}-0002`,
    '구간 이름이 함께 기록된다': (t) => t[0].step === 'store_nearby' && t[1].step === 'dish_detail',
    '서버가 우리가 보낸 값을 그대로 돌려준다': (t) => t.every((r) => r.requestId && r.requestId.startsWith(tag)),
  });

  // 접두사를 다시 정하면 일련번호와 흔적이 초기화된다(실행마다 1부터 시작해야 한다).
  setRequestIdPrefix(tag);
  check(getRequestTrail(), {
    '접두사를 다시 정하면 흔적이 비워진다': (t) => t.length === 0,
  });

  // 형식에 맞지 않는 접두사는 즉시 거부한다 — Gateway가 조용히 무시하면 조인이 깨진다.
  let rejected = false;
  try {
    setRequestIdPrefix('bad prefix!');
  } catch (_) {
    rejected = true;
  }
  check(rejected, { '형식에 안 맞는 접두사는 거부한다': (r) => r === true });
}
