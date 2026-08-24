import { check } from 'k6';

import {
  decodeJwtExpirationMs,
  refreshIfExpiring,
  seedCredentials,
} from './lib/accounts.js';
import { errorCodeOf } from './lib/api.js';
import {
  TIME_WINDOWS,
  buildDailySellerSpecs,
  buildTargetSellerSpecs,
  orderableWindowKeysAt,
  stressRatesForDay,
} from './lib/long-run-config.js';
import { selectOldestNewReservedOrder } from './lib/order-selection.js';

export const options = {
  vus: 1,
  iterations: 1,
  thresholds: {
    checks: ['rate==1'],
  },
};

function uniqueCount(rows, selector) {
  return new Set(rows.map(selector)).size;
}

function kstDateAtMinute(minute) {
  const kstMidnightUtc = Date.UTC(2026, 7, 23, 15, 0, 0);
  return new Date(kstMidnightUtc + minute * 60 * 1000);
}

export default function () {
  const daily = buildDailySellerSpecs('20260824');
  const target = buildTargetSellerSpecs('20260828', 5);
  const jwtWithOneHourExpiration = 'eyJhbGciOiJub25lIn0.eyJleHAiOjM2MDB9.';
  const credentials = seedCredentials(1);
  const stableSession = { accessTokenExpiresAtMs: 120000 };

  check(decodeJwtExpirationMs(jwtWithOneHourExpiration), {
    'JWT exp 초를 밀리초로 변환': (value) => value === 3600000,
  });
  check(credentials, {
    '시드 계정 번호를 로그인 자격 증명으로 변환': (value) =>
      value.accountNo === 1 &&
      value.email === 'seller001@seed.lastdish.kr' &&
      value.password === __ENV.SEED_PASSWORD,
  });
  check(refreshIfExpiring(stableSession, 0), {
    '만료까지 60초 초과면 refresh 없이 같은 세션 유지': (value) => value === stableSession,
  });
  check(errorCodeOf({ body: JSON.stringify({ error: { code: 'AUTH001' } }) }), {
    'API 오류 응답에서 코드 추출': (value) => value === 'AUTH001',
  });

  check(daily, {
    '일일 판매자 40개': (rows) => rows.length === 40,
    '유형별 판매자 10개': (rows) =>
      TIME_WINDOWS.every(
        (window) => rows.filter((row) => row.windowKey === window.key).length === 10,
      ),
    '일일 공통 키 고유': (rows) => uniqueCount(rows, (row) => row.key) === 40,
    '일일 이메일 고유': (rows) => uniqueCount(rows, (row) => row.email) === 40,
    '일일 사용자명 고유': (rows) => uniqueCount(rows, (row) => row.userName) === 40,
    '일일 전화번호 고유': (rows) => uniqueCount(rows, (row) => row.phone) === 40,
    '일일 사업자번호 고유': (rows) =>
      uniqueCount(rows, (row) => row.store.businessNumber) === 40,
    '사용자명 4~20자': (rows) =>
      rows.every((row) => row.userName.length >= 4 && row.userName.length <= 20),
    '비밀번호를 설계도에 저장하지 않음': (rows) =>
      rows.every(
        (row) =>
          !Object.prototype.hasOwnProperty.call(row, 'password') &&
          !Object.prototype.hasOwnProperty.call(row, 'accessToken') &&
          !Object.prototype.hasOwnProperty.call(row, 'refreshToken'),
      ),
  });

  check(target, {
    '5일차 목표 판매자 200개': (rows) => rows.length === 200,
    '5일차 목표 공통 키 고유': (rows) => uniqueCount(rows, (row) => row.key) === 200,
    '5일차 목표 이메일 고유': (rows) => uniqueCount(rows, (row) => row.email) === 200,
    '5일차 목표 사용자명 고유': (rows) => uniqueCount(rows, (row) => row.userName) === 200,
    '5일차 목표 전화번호 고유': (rows) => uniqueCount(rows, (row) => row.phone) === 200,
  });

  const selectedOrder = selectOldestNewReservedOrder(
    [
      { orderId: 750005, status: 'RESERVED', phone: '010-A' },
      { orderId: 750002, status: 'RESERVED', phone: '010-B' },
      { orderId: 750001, status: 'PICKUP_READY', phone: '010-C' },
      { orderId: 700000, status: 'RESERVED', phone: '010-D' },
    ],
    750000,
  );
  const noSelectedOrder = selectOldestNewReservedOrder(
    [
      { orderId: 750000, status: 'RESERVED' },
      { orderId: 750001, status: 'PICKUP_READY' },
    ],
    750000,
  );

  check(selectedOrder, {
    '전화번호와 무관하게 가장 오래된 새 RESERVED 선택': (order) =>
      order && order.orderId === 750002,
  });
  check(noSelectedOrder, {
    '새 RESERVED 주문이 없으면 null': (order) => order === null,
  });

  for (let minute = 0; minute < 24 * 60; minute += 30) {
    check(orderableWindowKeysAt(kstDateAtMinute(minute)), {
      [`KST ${String(Math.floor(minute / 60)).padStart(2, '0')}:${String(minute % 60).padStart(2, '0')} 주문 가능 유형 존재`]:
        (keys) => keys.length >= 1,
    });
  }

  for (let campaignDay = 1; campaignDay <= 5; campaignDay += 1) {
    const rates = stressRatesForDay(campaignDay);
    check(rates, {
      [`${campaignDay}일차 스트레스 흐름 합계 일치`]: (value) =>
        value.browse + value.purchase + value.seller + value.stock === value.total,
    });
  }

  console.log('순수 모델 검증 완료: HTTP 요청 0회');
}
