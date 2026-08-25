import { sleep } from 'k6';

import { decodeJwtExpirationMs, decodeJwtRole } from './accounts.js';
import { apiGet, apiSend, dataOf, errorCodeOf } from './api.js';

const ACCOUNT_NOT_FOUND = 'A002';
const DUPLICATE_SIGNUP_CODES = new Set(['A006', 'A007']);
const DISH_ALREADY_EXISTS = 'D006';
const SELLER_ROLE_RETRY = 30;
const SELLER_ROLE_RETRY_WAIT_SECONDS = 1;

function fail(message) {
  throw new Error(`판매자 생애주기 검증 실패: ${message}`);
}

function sameNumber(left, right) {
  return Number(left) === Number(right);
}

function sameCoordinate(left, right) {
  return Math.abs(Number(left) - Number(right)) < 0.000001;
}

function sameStringSet(left, right) {
  return JSON.stringify([...(left || [])].sort()) === JSON.stringify([...(right || [])].sort());
}

// 결정적 spec을 현재 회원·매장·상품 DTO와 일치하는 세 payload로 바꾼다.
export function buildLifecyclePayloads(spec, password, storeId, imageKey) {
  return {
    signup: {
      userName: spec.userName,
      password,
      name: spec.name,
      phone: spec.phone,
      email: spec.email,
    },
    store: {
      storeName: spec.store.storeName,
      businessNumber: spec.store.businessNumber,
      storeAddress: spec.store.storeAddress,
      storePhone: spec.store.storePhone,
      openTime: spec.store.openTime,
      closeTime: spec.store.closeTime,
      latitude: spec.store.latitude,
      longitude: spec.store.longitude,
      category: spec.store.category,
      holidays: spec.store.holidays,
    },
    dish: {
      storeId,
      dishName: spec.dish.dishName,
      registeredAt: spec.dish.registeredAt,
      description: spec.dish.description,
      category: spec.dish.category,
      imageKey,
      stockQuantity: spec.dish.stockQuantity,
      dishPrice: spec.dish.dishPrice,
      discountPrice: spec.dish.discountPrice,
      pickupStartTime: spec.dish.pickupStartTime,
      pickupEndTime: spec.dish.pickupEndTime,
    },
  };
}

function validateProfileIdentity(spec, profile) {
  if (!profile || !profile.id) {
    fail(`${spec.email} 회원 응답이 없습니다.`);
  }
  if (profile.email !== spec.email || profile.userName !== spec.userName) {
    fail(`${spec.email} 회원 식별자가 결정적 spec과 다릅니다.`);
  }
}

function validateStoreIdentity(spec, profile, store) {
  if (!store || !store.storeId) {
    fail(`${spec.email} 매장 응답이 없습니다.`);
  }
  if (!sameNumber(store.memberId, profile.id)) {
    fail(`${spec.email} 매장 소유자가 현재 회원과 다릅니다.`);
  }
  if (
    store.storeName !== spec.store.storeName ||
    store.businessNumber !== spec.store.businessNumber ||
    store.storeAddress !== spec.store.storeAddress ||
    store.storePhone !== spec.store.storePhone ||
    store.openTime !== spec.store.openTime ||
    store.closeTime !== spec.store.closeTime ||
    !sameCoordinate(store.latitude, spec.store.latitude) ||
    !sameCoordinate(store.longitude, spec.store.longitude) ||
    store.category !== spec.store.category ||
    !sameStringSet(store.holidays, spec.store.holidays)
  ) {
    fail(`${spec.email} 매장 식별자 또는 영업시간이 결정적 spec과 다릅니다.`);
  }
}

function validateDishIdentity(spec, store, dish) {
  if (!dish || !dish.dishId) {
    fail(`${spec.email} 상품 응답이 없습니다.`);
  }
  if (!sameNumber(dish.storeId, store.storeId) || dish.dishName !== spec.dish.dishName) {
    fail(`${spec.email} 상품 소유권 또는 이름이 결정적 spec과 다릅니다.`);
  }
}

// Gateway에서 본인 매장 조회는 SELLER 토큰에만 허용된다.
export function canReadOwnedStore(profile) {
  return Boolean(profile && profile.role === 'SELLER');
}

// 서버에서 다시 확인한 세 엔티티만 비밀값 없는 당일 manifest 항목으로 바꾼다.
export function validateLifecycleResult(spec, profile, store, dish) {
  validateProfileIdentity(spec, profile);
  if (profile.role !== 'SELLER') {
    fail(`${spec.email} 역할이 SELLER가 아닙니다: ${profile.role}`);
  }
  validateStoreIdentity(spec, profile, store);
  validateDishIdentity(spec, store, dish);

  return {
    key: spec.key,
    windowKey: spec.windowKey,
    slot: spec.slot,
    email: spec.email,
    memberId: profile.id,
    storeId: store.storeId,
    dishId: dish.dishId,
    openTime: spec.store.openTime,
    closeTime: spec.store.closeTime,
    pickupStartTime: spec.dish.pickupStartTime,
    pickupEndTime: spec.dish.pickupEndTime,
  };
}

function loginForLifecycle(credentials) {
  const response = apiSend('auth_login', 'POST', '/auth/login', null, credentials);
  const tokens = dataOf(response);
  if (!tokens || !tokens.accessToken || !tokens.refreshToken) {
    return { response, errorCode: errorCodeOf(response), session: null, profile: null };
  }

  const profileResponse = apiGet('member_me', '/members/me', tokens.accessToken);
  const profile = dataOf(profileResponse);
  if (!profile) {
    fail(`${credentials.email} 로그인 후 회원 정보를 조회하지 못했습니다.`);
  }

  return {
    response,
    errorCode: null,
    profile,
    session: {
      email: credentials.email,
      accessToken: tokens.accessToken,
      refreshToken: tokens.refreshToken,
      accessTokenExpiresAtMs: decodeJwtExpirationMs(tokens.accessToken),
      token: tokens.accessToken,
      memberId: profile.id,
      role: profile.role,
    },
  };
}

function ensureMember(spec, password) {
  const credentials = { email: spec.email, password };
  let login = loginForLifecycle(credentials);

  if (!login.session) {
    if (login.errorCode !== ACCOUNT_NOT_FOUND) {
      fail(`${spec.email} 로그인 실패: code=${login.errorCode || 'UNKNOWN'}`);
    }

    const signupPayload = buildLifecyclePayloads(spec, password, null, null).signup;
    const signupResponse = apiSend('lifecycle_signup', 'POST', '/auth/signup', null, signupPayload);
    const signup = dataOf(signupResponse);
    const signupError = errorCodeOf(signupResponse);
    if (!signup && !DUPLICATE_SIGNUP_CODES.has(signupError)) {
      fail(`${spec.email} 회원가입 실패: code=${signupError || 'UNKNOWN'}`);
    }

    login = loginForLifecycle(credentials);
    if (!login.session) {
      fail(`${spec.email} 회원가입 후 로그인 실패: code=${login.errorCode || 'UNKNOWN'}`);
    }
  }

  validateProfileIdentity(spec, login.profile);
  return login;
}

// 프로필과 토큰이 둘 다 SELLER여야 SELLER 전용 경로를 호출할 수 있다.
// Gateway는 토큰만 보므로 프로필만 맞으면 403이 난다.
function hasUsableSellerToken(profile, token) {
  return canReadOwnedStore(profile) && decodeJwtRole(token) === 'SELLER';
}

function findOwnedStore(spec, profile, token) {
  if (!hasUsableSellerToken(profile, token)) {
    return null;
  }

  const response = apiGet('seller_stores_for_dish', '/stores/mine', token);
  const stores = dataOf(response);
  if (!Array.isArray(stores)) {
    fail(
      `${spec.email} 매장 목록 조회 실패: status=${response.status} code=${errorCodeOf(response) || 'UNKNOWN'}`,
    );
  }
  if (stores.length > 1) {
    fail(`${spec.email} 소유 매장이 ${stores.length}개라 하나를 결정할 수 없습니다.`);
  }
  if (stores.length === 0) {
    return null;
  }
  validateStoreIdentity(spec, profile, stores[0]);
  return stores[0];
}

function ensureStore(spec, login, password) {
  let store = findOwnedStore(spec, login.profile, login.session.accessToken);
  let sellerLogin = hasUsableSellerToken(login.profile, login.session.accessToken) ? login : null;
  if (!store) {
    const storePayload = buildLifecyclePayloads(spec, password, null, null).store;
    const createResponse = apiSend(
      'lifecycle_store_create',
      'POST',
      '/stores',
      login.session.accessToken,
      storePayload,
    );
    store = dataOf(createResponse);
    if (!store) {
      // 직전 실행이 매장 생성 직후 중단됐다면 SELLER 이벤트 반영을 기다린 뒤 기존 매장을 복구한다.
      sellerLogin = waitForSellerRole(spec, password);
      store = findOwnedStore(spec, sellerLogin.profile, sellerLogin.session.accessToken);
    }
    validateStoreIdentity(spec, login.profile, store);
  }

  return { sellerLogin, store };
}

// 매장 생성 뒤 SELLER 권한이 실제로 쓸 수 있는 토큰에 담길 때까지 재로그인한다.
//
// 프로필(/members/me)이 SELLER여도 같은 로그인이 준 토큰은 아직 MEMBER일 수 있다.
// 역할 반영이 비동기라 로그인 시점과 프로필 조회 시점 사이에 바뀌기 때문이다.
// Gateway는 토큰만 보므로 프로필로 판단하면 POST /dishes가 403 G002로 거절된다.
// 따라서 토큰 자체의 role이 SELLER가 될 때까지 기다린다.
function waitForSellerRole(spec, password) {
  const credentials = { email: spec.email, password };
  let lastProfileRole = null;
  let lastTokenRole = null;

  for (let attempt = 0; attempt <= SELLER_ROLE_RETRY; attempt += 1) {
    if (attempt > 0) {
      sleep(SELLER_ROLE_RETRY_WAIT_SECONDS);
    }
    const login = loginForLifecycle(credentials);
    if (!login.session) {
      fail(`${spec.email} SELLER 확인용 재로그인 실패: code=${login.errorCode || 'UNKNOWN'}`);
    }
    validateProfileIdentity(spec, login.profile);

    lastProfileRole = login.profile.role;
    lastTokenRole = decodeJwtRole(login.session.accessToken);

    if (lastProfileRole === 'SELLER' && lastTokenRole === 'SELLER') {
      return login;
    }
  }

  fail(
    `${spec.email} 역할이 ${SELLER_ROLE_RETRY}초 안에 SELLER로 동기화되지 않았습니다: ` +
      `프로필=${lastProfileRole} 토큰=${lastTokenRole}`,
  );
}

function findOwnedDish(spec, store, token) {
  const dishes = dataOf(apiGet('seller_dish_list', `/dishes?storeId=${store.storeId}`, token)) || [];
  const matching = dishes.filter((dish) => dish.dishName === spec.dish.dishName);
  if (matching.length > 1) {
    fail(`${spec.email} 결정적 이름의 상품이 ${matching.length}개입니다.`);
  }
  if (matching.length === 1) {
    validateDishIdentity(spec, store, matching[0]);
    return matching[0];
  }
  if (dishes.length > 0) {
    fail(`${spec.email} 매장에 다른 상품이 있어 결정적 상품을 생성할 수 없습니다.`);
  }
  return null;
}

function ensureDish(spec, store, login) {
  let dish = findOwnedDish(spec, store, login.session.accessToken);
  if (dish) {
    return dish;
  }

  const dishPayload = buildLifecyclePayloads(spec, null, store.storeId, null).dish;
  const createResponse = apiSend(
    'lifecycle_dish_create',
    'POST',
    '/dishes',
    login.session.accessToken,
    dishPayload,
  );
  dish = dataOf(createResponse);
  if (!dish && errorCodeOf(createResponse) === DISH_ALREADY_EXISTS) {
    dish = findOwnedDish(spec, store, login.session.accessToken);
  }
  validateDishIdentity(spec, store, dish);
  return dish;
}

// 회원→매장→SELLER 토큰→이미지 없는 상품을 조회 우선으로 재구성한다.
export function ensureSellerLifecycle(spec) {
  const password = __ENV.LOADTEST_PASSWORD;
  if (!password || password === 'change-me-before-data-creation') {
    fail('LOADTEST_PASSWORD를 실제 준비용 값으로 설정해야 합니다.');
  }
  const initialLogin = ensureMember(spec, password);
  const preparedStore = ensureStore(spec, initialLogin, password);
  const store = preparedStore.store;
  const sellerLogin = preparedStore.sellerLogin || waitForSellerRole(spec, password);
  const dish = ensureDish(spec, store, sellerLogin);
  return validateLifecycleResult(spec, sellerLogin.profile, store, dish);
}
