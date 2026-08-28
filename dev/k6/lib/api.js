// HTTP 호출 한 겹. 구간 이름표(tags.name), 구간별 응답시간, 성공 판정, 반복별 호출 수를 한곳에서 처리한다.
import http from 'k6/http';
import { check } from 'k6';
import { API, REQUEST_TIMEOUT } from './config.js';
import { expectedBusinessOutcomes, infrastructureFailures, stepTrend } from './metrics.js';

let requestCount = 0;

// 반복 시작 시점에 호출 수를 0으로 되돌린다.
export function resetRequestCount() {
  requestCount = 0;
}

export function getRequestCount() {
  return requestCount;
}

// 요청마다 고유한 X-Request-Id를 붙여 서버 로그의 queryCount와 1:1로 잇기 위한 상태.
// 접두사를 정하지 않으면 헤더를 붙이지 않는다 — 기존 시나리오의 동작은 그대로다.
let requestIdPrefix = null;
let requestIdSeq = 0;
const requestTrail = [];

// Gateway가 인바운드 X-Request-Id를 형식 검증 후 그대로 전파한다.
// RequestIdSupport.VALID_FORMAT = ^[A-Za-z0-9._-]{1,64}$ 이고 뒤에 -0001이 붙으므로
// 접두사는 55자로 제한한다. (2026-08-28 운영에서 왕복 확인함)
export function setRequestIdPrefix(prefix) {
  if (prefix && !/^[A-Za-z0-9._-]{1,55}$/.test(prefix)) {
    throw new Error(`X-Request-Id 접두사는 [A-Za-z0-9._-] 55자 이하여야 합니다: ${prefix}`);
  }
  requestIdPrefix = prefix || null;
  requestIdSeq = 0;
  requestTrail.length = 0;
}

// 요청 번호 → 구간 이름 대응표. 로그 쪽 queryCount와 조인할 때 쓴다.
export function getRequestTrail() {
  return requestTrail;
}

// 토큰이 있으면 Bearer 헤더를 붙인다. 공개 조회는 토큰 없이 보낸다.
export function authHeaders(token) {
  const headers = { 'Content-Type': 'application/json' };
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  // apiGet·apiSend·apiBatchGet이 전부 이 함수를 지나가므로 배치 안의 개별 요청도 각자 번호를 받는다.
  if (requestIdPrefix) {
    requestIdSeq += 1;
    headers['X-Request-Id'] = `${requestIdPrefix}-${String(requestIdSeq).padStart(4, '0')}`;
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
    if (parsed && typeof parsed === 'object' && 'success' in parsed) {
      if (parsed.success !== true) {
        return null;
      }
      return 'data' in parsed ? parsed.data : null;
    }
    return parsed;
  } catch (_) {
    return null;
  }
}

// 원본 또는 ApiResponse 봉투 형태의 예치금 응답에서 숫자 잔액을 꺼낸다.
export function depositBalanceOf(response) {
  const data = dataOf(response);
  if (!data || data.balance === null || data.balance === undefined) {
    return null;
  }
  const balance = Number(data.balance);
  return Number.isFinite(balance) ? balance : null;
}

// ApiResponse 오류 봉투에서 분류용 error.code만 안전하게 꺼낸다.
export function errorCodeOf(response) {
  if (!response || !response.body) {
    return null;
  }
  try {
    const parsed = JSON.parse(response.body);
    return parsed && parsed.error && parsed.error.code ? parsed.error.code : null;
  } catch (_) {
    return null;
  }
}

const EXPECTED_BUSINESS_OUTCOMES = {
  DEP001: 'deposit_exhausted',
  CT004: 'stock_exhausted',
  D001: 'stock_exhausted',
  D003: 'stock_exhausted',
  ORD007: 'stock_exhausted',
  ORD010: 'target_unavailable',
  ORD011: 'target_unavailable',
};

export function expectedBusinessOutcomeOf(response) {
  if (!response || response.status < 400 || response.status >= 500) {
    return null;
  }
  return EXPECTED_BUSINESS_OUTCOMES[errorCodeOf(response)] || null;
}

// 업무 4xx와 분리해 보호 중단에 사용할 네트워크·서버 실패만 판정한다.
export function infrastructureFailureOf(response) {
  return Boolean(response) && (response.status === 0 || response.status >= 500);
}

// 응답 하나를 검사하고 호출 수에 반영한다. 응답시간 기록 여부는 호출부가 정한다.
function observe(step, response, recordDuration) {
  requestCount += 1;

  // 응답 헤더에서 되읽는다 — 우리가 보낸 값이 무시됐다면 여기서 다른 값이 나오므로 그 자체가 검증이다.
  if (requestIdPrefix) {
    requestTrail.push({
      requestId: response.headers['X-Request-Id'] || null,
      step,
      url: response.url,
      status: response.status,
      durationMs: Math.round(response.timings.duration),
    });
  }

  infrastructureFailures.add(infrastructureFailureOf(response));
  if (recordDuration) {
    stepTrend(step).add(response.timings.duration);
  }

  const expectedOutcome = expectedBusinessOutcomeOf(response);
  if (expectedOutcome) {
    const code = errorCodeOf(response);
    expectedBusinessOutcomes.add(1, { reason: expectedOutcome, code });
    console.warn(`[${step}] 예상 업무 결과: reason=${expectedOutcome} code=${code}`);
    return true;
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
