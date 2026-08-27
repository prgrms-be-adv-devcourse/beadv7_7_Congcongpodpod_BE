import http from 'k6/http';
import { check, sleep } from 'k6';

import { buildSummaryHandler } from './lib/summary.js';

export const handleSummary = buildSummaryHandler();

const baseUrl = (__ENV.BASE_URL || '').replace(/\/$/, '');

if (!baseUrl) {
  throw new Error('BASE_URL 환경변수가 필요합니다.');
}

export const options = {
  vus: 1,
  iterations: 1,
  thresholds: {
    checks: ['rate==1'],
    http_req_failed: ['rate==0'],
    http_req_duration: ['p(95)<1000'],
  },
};

export default function () {
  const response = http.get(
    `${baseUrl}/api/v1/stores/nearby?latitude=37.5665&longitude=126.9780&page=0&size=1`,
    {
      tags: { name: 'Public store search' },
      timeout: '5s',
    },
  );

  const succeeded = check(response, {
    '공개 매장 조회 응답이 200이다': (result) => result.status === 200,
    'API 응답이 성공이다': (result) => {
      try {
        return result.json('success') === true;
      } catch (_) {
        return false;
      }
    },
  });

  if (!succeeded) {
    console.error(
      `공개 매장 조회 실패: status=${response.status}, body=${response.body.slice(0, 500)}`,
    );
  }

  sleep(1);
}
