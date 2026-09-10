import { check } from 'k6';

import {
  selectOldestNewReservedOrder,
  selectOldestPickupReadyOrder,
} from '../lib/order-selection.js';

export const options = {
  vus: 1,
  iterations: 1,
  thresholds: {
    checks: ['rate==1'],
  },
};

const MIN_ID = 750000;

export default function () {
  const rows = [
    { orderId: 750001, status: 'PICKUP_READY' },
    { orderId: 750002, status: 'PICKUP_READY' },
    { orderId: 750003, status: 'RESERVED' },
    { orderId: 749999, status: 'PICKUP_READY' }, // 이번 실행이 만든 주문이 아니다
    { orderId: 750004, status: 'PICKED_UP' },
  ];

  check(null, {
    '방금 수락한 주문은 픽업 대상에서 빠진다': () =>
      selectOldestPickupReadyOrder(rows, MIN_ID, 750001).orderId === 750002,

    '제외 대상이 없으면 가장 오래된 것을 고른다': () =>
      selectOldestPickupReadyOrder(rows, MIN_ID, null).orderId === 750001,

    'PICKUP_READY가 아닌 주문은 고르지 않는다': () =>
      selectOldestPickupReadyOrder(
        [{ orderId: 750010, status: 'RESERVED' }],
        MIN_ID,
        null,
      ) === null,

    '이번 실행 이전 주문은 고르지 않는다': () =>
      selectOldestPickupReadyOrder(
        [{ orderId: 749999, status: 'PICKUP_READY' }],
        MIN_ID,
        null,
      ) === null,

    '픽업할 것이 하나도 없으면 null이다 (첫 반복)': () =>
      selectOldestPickupReadyOrder(
        [{ orderId: 750001, status: 'PICKUP_READY' }],
        MIN_ID,
        750001,
      ) === null,

    '빈 목록과 undefined를 견딘다': () =>
      selectOldestPickupReadyOrder([], MIN_ID, null) === null &&
      selectOldestPickupReadyOrder(undefined, MIN_ID, null) === null,

    '수락 대상 선택은 그대로 RESERVED만 본다': () =>
      selectOldestNewReservedOrder(rows, MIN_ID).orderId === 750003,
  });
}
