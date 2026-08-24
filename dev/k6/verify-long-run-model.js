import { check } from 'k6';

import {
  TIME_WINDOWS,
  buildDailySellerSpecs,
  buildTargetSellerSpecs,
  orderableWindowKeysAt,
  stressRatesForDay,
} from './lib/long-run-config.js';

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
