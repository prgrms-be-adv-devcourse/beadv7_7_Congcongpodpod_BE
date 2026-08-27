import { orderableWindowKeysAt } from './long-run-config.js';

const SELLERS_PER_DAY = 40;

function fail(message) {
  throw new Error(`실행 상태 검증 실패: ${message}`);
}

function ensureExpected(expected) {
  if (!expected || !Number.isInteger(expected.campaignDay) || expected.campaignDay < 1) {
    fail(`CAMPAIGN_DAY가 올바르지 않습니다: ${expected && expected.campaignDay}`);
  }
  if (!expected.datasetEpoch) {
    fail('DATASET_EPOCH이 필요합니다.');
  }
}

function ensureUnique(sellers, field) {
  const values = sellers.map((seller) => seller[field]);
  if (values.some((value) => value === null || value === undefined || value === '')) {
    fail(`${field} 값이 비어 있습니다.`);
  }
  if (new Set(values).size !== values.length) {
    fail(`${field} 값이 중복됩니다.`);
  }
}

function ensurePoolLimit(name, value) {
  if (!Number.isInteger(value) || value < 0) {
    fail(`${name}는 0 이상의 정수여야 합니다: ${value}`);
  }
}

export function parseRunState(text, expected) {
  let state;
  try {
    state = JSON.parse(text);
  } catch (_) {
    fail('상태 파일이 올바른 JSON이 아닙니다.');
  }
  return validateRunState(state, expected);
}

export function validateRunState(state, expected) {
  ensureExpected(expected);
  if (!state || typeof state !== 'object') {
    fail('상태 파일 본문이 객체가 아닙니다.');
  }
  if (state.schemaVersion !== 1) {
    fail(`schemaVersion은 1이어야 합니다: ${state.schemaVersion}`);
  }
  if (state.campaignDay !== expected.campaignDay) {
    fail(`CAMPAIGN_DAY가 다릅니다: state=${state.campaignDay} env=${expected.campaignDay}`);
  }
  if (state.datasetEpoch !== expected.datasetEpoch) {
    fail(`DATASET_EPOCH이 다릅니다: state=${state.datasetEpoch} env=${expected.datasetEpoch}`);
  }
  if (state.reconstructedDataset !== true) {
    fail('reconstructedDataset이 true가 아닙니다.');
  }
  if (state.partial === true) {
    fail('partial 상태 파일은 장기 실행 입력으로 사용할 수 없습니다.');
  }
  if (!Array.isArray(state.sellers)) {
    fail('sellers 배열이 없습니다.');
  }

  const expectedCount = SELLERS_PER_DAY * expected.campaignDay;
  if (state.sellers.length !== expectedCount) {
    fail(`판매자 목표 개수가 다릅니다: actual=${state.sellers.length} expected=${expectedCount}`);
  }

  for (const field of ['email', 'memberId', 'storeId', 'dishId']) {
    ensureUnique(state.sellers, field);
  }
  return state;
}

export function loadRunState(
  path,
  expected = {
    campaignDay: Number(__ENV.CAMPAIGN_DAY),
    datasetEpoch: __ENV.DATASET_EPOCH,
  },
  date = new Date(),
) {
  if (!path) {
    fail('STATE_FILE 경로가 필요합니다.');
  }
  const state = parseRunState(open(path), expected);
  orderableTargetsAt(state, date);
  return state;
}

export function orderableTargetsAt(state, date) {
  if (!state || !Array.isArray(state.sellers)) {
    fail('주문 가능 대상을 찾을 sellers 배열이 없습니다.');
  }
  const orderableKeys = new Set(orderableWindowKeysAt(date));
  const targets = state.sellers.filter((seller) => orderableKeys.has(seller.windowKey));
  if (targets.length === 0) {
    fail('현재 시각에 주문 가능 대상이 없습니다.');
  }
  return targets;
}

export function purchaseTargetOf(seller) {
  const storeId = Number(seller && seller.storeId);
  const dishId = Number(seller && seller.dishId);
  if (!Number.isInteger(storeId) || storeId < 1 || !Number.isInteger(dishId) || dishId < 1) {
    fail('구매 대상에는 양의 정수 storeId와 dishId가 필요합니다.');
  }
  return { storeId, dishId };
}

export function partitionSellerPools(state, sellerVuLimit, stockVuLimit) {
  if (!state || !Array.isArray(state.sellers)) {
    fail('판매자 계정 배열이 없습니다.');
  }
  ensurePoolLimit('sellerVuLimit', sellerVuLimit);
  ensurePoolLimit('stockVuLimit', stockVuLimit);

  const required = sellerVuLimit + stockVuLimit;
  if (required > state.sellers.length) {
    fail(`판매자 계정이 부족합니다: required=${required} actual=${state.sellers.length}`);
  }

  ensureUnique(state.sellers, 'email');
  const orderSellers = state.sellers.slice(0, sellerVuLimit);
  const stockSellers = state.sellers.slice(sellerVuLimit, required);
  const activitySellers = state.sellers.slice(required);
  const orderEmails = new Set(orderSellers.map((seller) => seller.email));
  if (stockSellers.some((seller) => orderEmails.has(seller.email))) {
    fail('판매자 주문 처리 풀과 재고 풀의 email이 겹칩니다.');
  }

  return { orderSellers, stockSellers, activitySellers };
}
