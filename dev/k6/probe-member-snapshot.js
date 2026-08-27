// core-service의 member_snapshots에 시드 계정이 들어 있는지 계정별로 확인한다.
//
// OrderFacade.payAndCreateOrder는 스냅샷을 먼저 찾고 그다음 장바구니 항목을 찾는다.
// 존재하지 않는 cartItemId로 주문을 시도하면 둘 중 어디서 막히는지로 스냅샷 유무를 알 수 있다.
//
//   503 ORD013 (ORDER_MEMBER_SNAPSHOT_NOT_FOUND) → 스냅샷 없음
//   404 ORD006 (CART_ITEM_NOT_FOUND)             → 스냅샷 있음
//
// 두 경우 모두 쓰기 전에 예외로 롤백되므로 운영 데이터를 바꾸지 않는다.
import http from 'k6/http';
import { Rate } from 'k6/metrics';
import { depositBalanceOf, errorCodeOf } from './lib/api.js';
import { API, SEED, accountEmail } from './lib/config.js';

const FROM = Number(__ENV.PROBE_FROM || 1);
const TO = Number(__ENV.PROBE_TO || SEED.accountCount);
const MIN_BUYER_BALANCE = Number(__ENV.MIN_BUYER_BALANCE || 10000000);
const seedPrerequisiteReady = new Rate('seed_prerequisite_ready');

// 어떤 회원의 장바구니에도 없는 값이어야 한다. cart_items는 시드에서 비어 있고 시퀀스도 낮다.
const MISSING_CART_ITEM_ID = 999999999;

export const options = {
  scenarios: {
    default: {
      executor: 'shared-iterations',
      vus: 1,
      iterations: 1,
      maxDuration: '30m',
    },
  },
  // HTTP 404는 snapshot 존재를 판정하기 위한 의도된 응답이고 최종 준비 가능 여부는 이 Rate로 판정한다.
  thresholds: {
    seed_prerequisite_ready: ['rate==1'],
  },
};

// 연속된 번호를 1~150 같은 구간 문자열로 접는다.
function toRanges(numbers) {
  if (numbers.length === 0) {
    return '없음';
  }
  const ranges = [];
  let start = numbers[0];
  let previous = numbers[0];
  for (let i = 1; i <= numbers.length; i += 1) {
    const current = numbers[i];
    if (current !== previous + 1) {
      ranges.push(start === previous ? `${start}` : `${start}~${previous}`);
      start = current;
    }
    previous = current;
  }
  return ranges.join(', ');
}

export default function () {
  const headers = { 'Content-Type': 'application/json' };
  const present = [];
  const missing = [];
  const loginFailed = [];
  const unknown = [];
  const insufficientBalance = [];

  console.log(`member_snapshots 확인: seller${String(FROM).padStart(3, '0')} ~ seller${String(TO).padStart(3, '0')}`);

  for (let accountNo = FROM; accountNo <= TO; accountNo += 1) {
    const loginResponse = http.post(
      `${API}/auth/login`,
      JSON.stringify({ email: accountEmail(accountNo), password: SEED.password }),
      { headers, tags: { name: 'probe_login' }, timeout: '10s' },
    );

    let token = null;
    try {
      const parsed = JSON.parse(loginResponse.body);
      token = parsed && parsed.data ? parsed.data.accessToken : null;
    } catch (_) {
      token = null;
    }

    if (!token) {
      loginFailed.push(accountNo);
      seedPrerequisiteReady.add(false);
      continue;
    }

    const probeResponse = http.post(
      `${API}/orders/cartItems/${MISSING_CART_ITEM_ID}`,
      JSON.stringify({ dishPriceVersion: 0 }),
      {
        headers: Object.assign({ Authorization: `Bearer ${token}` }, headers),
        tags: { name: 'probe_order' },
        timeout: '10s',
      },
    );

    const code = errorCodeOf(probeResponse);
    if (code === 'ORD013') {
      missing.push(accountNo);
      seedPrerequisiteReady.add(false);
    } else if (code === 'ORD006') {
      present.push(accountNo);
      if (accountNo <= SEED.fundedAccountCount) {
        const balanceResponse = http.get(`${API}/deposits/balance`, {
          headers: Object.assign({ Authorization: `Bearer ${token}` }, headers),
          tags: { name: 'probe_deposit_balance' },
          timeout: '10s',
        });
        const balance = depositBalanceOf(balanceResponse);
        if (!Number.isFinite(balance) || balance < MIN_BUYER_BALANCE) {
          insufficientBalance.push(accountNo);
          seedPrerequisiteReady.add(false);
        } else {
          seedPrerequisiteReady.add(true);
        }
      } else {
        seedPrerequisiteReady.add(true);
      }
    } else {
      unknown.push(accountNo);
      seedPrerequisiteReady.add(false);
      console.warn(
        `seller${String(accountNo).padStart(3, '0')} 판정 불가: status=${probeResponse.status} code=${code} body=${String(probeResponse.body).slice(0, 200)}`,
      );
    }
  }

  const total = TO - FROM + 1;
  console.log('--- member_snapshots 확인 결과 ---');
  console.log(`대상 계정: ${total}개`);
  console.log(`스냅샷 있음 (ORD006): ${present.length}개 — ${toRanges(present)}`);
  console.log(`스냅샷 없음 (ORD013): ${missing.length}개 — ${toRanges(missing)}`);
  console.log(`로그인 실패: ${loginFailed.length}개 — ${toRanges(loginFailed)}`);
  console.log(`판정 불가: ${unknown.length}개 — ${toRanges(unknown)}`);
  console.log(
    `예치금 ${MIN_BUYER_BALANCE}원 미만 또는 조회 실패: ${insufficientBalance.length}개 — ${toRanges(insufficientBalance)}`,
  );
}
