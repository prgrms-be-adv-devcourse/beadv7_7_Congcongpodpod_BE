// HTTP 호출 한 겹. 구간 이름표(tags.name), 구간별 응답시간, 성공 판정, 반복별 호출 수를 한곳에서 처리한다.
import http from 'k6/http';
import { check } from 'k6';
import { API, REQUEST_TIMEOUT } from './config.js';
import { stepTrend } from './metrics.js';

let requestCount = 0;

// 반복 시작 시점에 호출 수를 0으로 되돌린다.
export function resetRequestCount() {
  requestCount = 0;
}

export function getRequestCount() {
  return requestCount;
}

// 토큰이 있으면 Bearer 헤더를 붙인다. 공개 조회는 토큰 없이 보낸다.
export function authHeaders(token) {
  const headers = { 'Content-Type': 'application/json' };
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  return headers;
}

// ApiResponse 봉투의 success=false를 HTTP 2xx와 별개로 실패로 본다.
function bodyIsNotFailure(response) {
  if (!response.body) {
    return true;
  }
  try {
    const parsed = JSON.parse(response.body);
    return !(parsed && typeof parsed === 'object' && parsed.success === false);
  } catch (_) {
    // JSON이 아니면 API 응답이 아니다 (게이트웨이 오류 페이지 등)
    return false;
  }
}

// ApiResponse 봉투면 data를, 봉투가 없는 응답이면 본문 자체를 돌려준다.
export function dataOf(response) {
  if (!response || !response.body) {
    return null;
  }
  try {
    const parsed = JSON.parse(response.body);
    if (parsed && typeof parsed === 'object' && 'success' in parsed && 'data' in parsed) {
      return parsed.data;
    }
    return parsed;
  } catch (_) {
    return null;
  }
}

// 응답 하나를 검사하고 호출 수에 반영한다. 응답시간 기록 여부는 호출부가 정한다.
function observe(step, response, recordDuration) {
  requestCount += 1;
  if (recordDuration) {
    stepTrend(step).add(response.timings.duration);
  }

  const ok = check(response, {
    [`${step} 2xx`]: (r) => r.status >= 200 && r.status < 300,
    [`${step} 본문 성공`]: (r) => bodyIsNotFailure(r),
  });

  if (!ok) {
    console.error(
      `[${step}] status=${response.status} url=${response.url} body=${String(response.body).slice(0, 300)}`,
    );
  }
  return ok;
}

export function apiGet(step, path, token) {
  const response = http.get(`${API}${path}`, {
    headers: authHeaders(token),
    tags: { name: step },
    timeout: REQUEST_TIMEOUT,
  });
  observe(step, response, true);
  return response;
}

export function apiSend(step, method, path, token, body) {
  const response = http.request(method, `${API}${path}`, body ? JSON.stringify(body) : null, {
    headers: authHeaders(token),
    tags: { name: step },
    timeout: REQUEST_TIMEOUT,
  });
  observe(step, response, true);
  return response;
}

// RN이 Promise.all로 동시에 보내는 구간을 http.batch로 그대로 옮긴다.
// 응답시간은 요청별이 아니라 묶음 전체의 벽시계 대기로 기록한다 — 사용자가 실제로 기다리는 시간이 그것이다.
export function apiBatchGet(step, paths, token) {
  if (paths.length === 0) {
    return [];
  }

  const requests = paths.map((path) => ({
    method: 'GET',
    url: `${API}${path}`,
    params: { headers: authHeaders(token), tags: { name: step }, timeout: REQUEST_TIMEOUT },
  }));

  const startedAt = Date.now();
  const responses = http.batch(requests);
  stepTrend(step).add(Date.now() - startedAt);

  for (const response of responses) {
    observe(step, response, false);
  }
  return responses;
}
