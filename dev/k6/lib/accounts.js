import encoding from 'k6/encoding';

import { apiGet, apiSend, dataOf, errorCodeOf } from './api.js';
import { SEED, accountEmail } from './config.js';

const REFRESH_EARLY_MS = 60 * 1000;

// VU 번호를 시드 계정 번호로 바꾼다.
//
// __VU는 테스트 전역에서 유일하므로, 대역 크기가 전역 VU 상한보다 작으면 나머지 연산이
// 한 바퀴 돌아 서로 다른 VU가 같은 계정을 잡는다. 계정 하나에 장바구니 항목이 하나뿐이라
// (CartService.addItem의 upsert) 그 순간 한쪽의 항목이 덮어써진다.
export function seedAccountNoForVu(startAccount, accountCount, vuId) {
  return startAccount + ((vuId - 1) % accountCount);
}

// JWT payload의 exp(초)를 k6에서 비교할 수 있는 epoch millisecond로 바꾼다.
export function decodeJwtExpirationMs(token) {
  const parts = String(token || '').split('.');
  if (parts.length !== 3 || !parts[1]) {
    throw new Error('Access Token이 JWT 형식이 아닙니다.');
  }

  let payload;
  try {
    payload = JSON.parse(encoding.b64decode(parts[1], 'rawurl', 's'));
  } catch (_) {
    throw new Error('Access Token JWT payload를 해석할 수 없습니다.');
  }

  const expirationSeconds = Number(payload.exp);
  if (!Number.isFinite(expirationSeconds) || expirationSeconds <= 0) {
    throw new Error('Access Token JWT payload에 유효한 exp가 없습니다.');
  }
  return expirationSeconds * 1000;
}

// JWT payload에 실제로 박힌 역할을 읽는다.
//
// /members/me의 역할과 토큰의 역할은 다를 수 있다. 매장 생성 뒤 역할이 SELLER로 바뀌는데
// 로그인 시점에 아직 반영되지 않았으면 토큰에는 MEMBER가 박히고, 그 직후 호출한
// /members/me는 이미 SELLER를 돌려준다. Gateway는 토큰만 보므로 이 차이가 403이 된다.
// 권한을 판단할 때는 프로필이 아니라 이 값을 봐야 한다.
export function decodeJwtRole(token) {
  const parts = String(token || '').split('.');
  if (parts.length !== 3 || !parts[1]) {
    return null;
  }

  try {
    const payload = JSON.parse(encoding.b64decode(parts[1], 'rawurl', 's'));
    if (typeof payload.role === 'string') {
      return payload.role;
    }
    // 배포에 따라 authorities/scope로 들어올 수 있어 함께 본다.
    const authorities = payload.authorities || payload.scope || payload.roles;
    if (typeof authorities === 'string') {
      return authorities.replace('ROLE_', '');
    }
    if (Array.isArray(authorities) && authorities.length > 0) {
      return String(authorities[0]).replace('ROLE_', '');
    }
    return null;
  } catch (_) {
    return null;
  }
}

// 시드 계정 번호를 로그인 입력으로 바꾸고 번호 범위를 즉시 검증한다.
export function seedCredentials(accountNo) {
  if (!Number.isInteger(accountNo) || accountNo < 1 || accountNo > SEED.accountCount) {
    throw new Error(`시드 계정 번호는 1~${SEED.accountCount} 정수여야 합니다: ${accountNo}`);
  }
  return {
    accountNo,
    email: accountEmail(accountNo),
    password: SEED.password,
  };
}

// 로그인 뒤 토큰 만료·회원·장바구니 정보를 한 세션 객체로 묶는다.
export function loginWithCredentials(credentials) {
  const loginResponse = apiSend('auth_login', 'POST', '/auth/login', null, {
    email: credentials.email,
    password: credentials.password,
  });
  const tokens = dataOf(loginResponse);
  if (!tokens || !tokens.accessToken || !tokens.refreshToken) {
    throw new Error(
      `로그인 실패: ${credentials.email} status=${loginResponse.status} code=${errorCodeOf(loginResponse) || 'UNKNOWN'}`,
    );
  }

  const profile = dataOf(apiGet('member_me', '/members/me', tokens.accessToken));
  if (!profile) {
    throw new Error(`회원 조회 실패: ${credentials.email}`);
  }
  const cart = dataOf(apiGet('cart_get', '/carts/members', tokens.accessToken));

  return {
    accountNo: credentials.accountNo || null,
    email: credentials.email,
    accessToken: tokens.accessToken,
    refreshToken: tokens.refreshToken,
    accessTokenExpiresAtMs: decodeJwtExpirationMs(tokens.accessToken),
    token: tokens.accessToken,
    memberId: profile.id,
    role: profile.role,
    cartId: cart ? cart.cartId : null,
    cartItems: cart && cart.items ? cart.items : [],
    storeId: null,
  };
}

// 만료 60초 전부터 refresh token을 한 번 회전시키고 호환용 token 별칭도 함께 갱신한다.
export function refreshIfExpiring(session, nowMs = Date.now()) {
  if (session.accessTokenExpiresAtMs - nowMs > REFRESH_EARLY_MS) {
    return session;
  }

  const refreshResponse = apiSend('auth_refresh', 'POST', '/auth/refresh', null, {
    refreshToken: session.refreshToken,
  });
  const tokens = dataOf(refreshResponse);
  if (!tokens || !tokens.accessToken || !tokens.refreshToken) {
    throw new Error(
      `토큰 갱신 실패: ${session.email} status=${refreshResponse.status} code=${errorCodeOf(refreshResponse) || 'UNKNOWN'}`,
    );
  }

  session.accessToken = tokens.accessToken;
  session.refreshToken = tokens.refreshToken;
  session.accessTokenExpiresAtMs = decodeJwtExpirationMs(tokens.accessToken);
  session.token = tokens.accessToken;
  return session;
}
