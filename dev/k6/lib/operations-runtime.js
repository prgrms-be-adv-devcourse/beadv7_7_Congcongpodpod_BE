import exec from 'k6/execution';

import {
  loginWithCredentials,
  refreshIfExpiring,
  seedAccountNoForVu,
  seedCredentials,
} from './accounts.js';
import { BUYER_ACCOUNT_POOLS } from './config.js';
import {
  buyerBrowse,
  buyerPurchase,
  clearLeftoverCartItem,
  sellerAdjustStock,
  sellerHandleOrder,
} from './flow.js';
import * as metrics from './metrics.js';
import {
  partitionAccountsByVu,
  selectWeightedTarget,
  targetDistributionFromEnv,
} from './operations-config.js';
import { loadRunState, orderableTargetsAt, partitionSellerPools } from './run-state.js';

export function buildOperationsAccountModel(state, { sellerVuLimit, stockVuLimit }) {
  const sellerPools = partitionSellerPools(state, sellerVuLimit, stockVuLimit);
  const orderSellerAccounts = sellerPools.orderSellers.concat(sellerPools.activitySellers);
  const stockSellerAccounts = sellerPools.stockSellers;

  return {
    orderSellerAccounts,
    stockSellerAccounts,
    sellerGroups: partitionAccountsByVu(orderSellerAccounts, sellerVuLimit),
    stockGroups: partitionAccountsByVu(stockSellerAccounts, stockVuLimit),
  };
}

export function orderableSellerCandidates(sellers, date = new Date()) {
  return orderableTargetsAt({ sellers }, date);
}

export function buildOrderableSellerGroups(sellers, sellerVuLimit, date = new Date()) {
  const candidates = orderableSellerCandidates(sellers, date);
  return partitionAccountsByVu(candidates, Math.min(sellerVuLimit, candidates.length));
}

export function sellerSlotIndex(scenarioName, groupCount) {
  const match = /^seller_(\d+)$/.exec(String(scenarioName || ''));
  if (!match) {
    return null;
  }

  const index = Number(match[1]) - 1;
  if (!Number.isInteger(index) || index < 0 || index >= groupCount) {
    throw new Error(`판매자 슬롯이 계정 그룹 범위를 벗어났습니다: ${scenarioName}`);
  }
  return index;
}

export function createOperationsRuntime({ sellerVuLimit, stockVuLimit, calibration = false }) {
  const loadtestPassword = __ENV.LOADTEST_PASSWORD;
  if (!loadtestPassword || loadtestPassword === 'change-me-before-data-creation') {
    throw new Error('LOADTEST_PASSWORD를 실제 준비용 값으로 설정해야 합니다.');
  }

  const runState = loadRunState(__ENV.STATE_FILE);
  const accountModel = buildOperationsAccountModel(runState, { sellerVuLimit, stockVuLimit });
  // 주문 대상 분포는 주문 흐름에만 적용한다. 조회는 두 실험군에서 똑같이 두어야
  // "집중도만 바꿨다"고 말할 수 있다.
  const orderDistribution = targetDistributionFromEnv();
  const calibrationSeller = calibration
    ? orderableTargetsAt({ sellers: accountModel.orderSellerAccounts }, new Date())[0]
    : null;
  const sessions = {};
  let sellerCursor = 0;
  let stockCursor = 0;

  function sessionFor(credentials, clearCartOnFirstLogin = false) {
    let session = sessions[credentials.email];
    if (!session) {
      session = loginWithCredentials(credentials);
      sessions[credentials.email] = session;
      if (clearCartOnFirstLogin) {
        clearLeftoverCartItem(session);
      }
    }
    return refreshIfExpiring(session);
  }

  function seedSession(pool, clearCartOnFirstLogin = false) {
    const accountNo = seedAccountNoForVu(pool.start, pool.count, __VU);
    return sessionFor(seedCredentials(accountNo), clearCartOnFirstLogin);
  }

  function groupForVu(groups) {
    return groups[(__VU - 1) % groups.length];
  }

  function nextSeller(group, cursor) {
    if (!group || group.length === 0) {
      throw new Error(`scenario=${exec.scenario.name}에 배정된 판매자 계정이 없습니다.`);
    }
    return group[cursor % group.length];
  }

  function loadtestSession(seller) {
    return sessionFor({ email: seller.email, password: loadtestPassword });
  }

  function currentOrderableTargets(sellers) {
    return orderableTargetsAt({ sellers }, new Date());
  }

  function browseFlow() {
    const session = seedSession(BUYER_ACCOUNT_POOLS.browse);
    const target = selectWeightedTarget(currentOrderableTargets(runState.sellers));
    buyerBrowse(session, target);
    metrics.browseIterations.add(1);
  }

  function purchaseFlow() {
    const session = seedSession(BUYER_ACCOUNT_POOLS.purchase, true);
    const candidates = calibration
      ? [calibrationSeller]
      : currentOrderableTargets(accountModel.orderSellerAccounts);
    const target = selectWeightedTarget(candidates, Math.random(), orderDistribution);
    buyerPurchase(session, target);
    metrics.purchaseIterations.add(1);
  }

  function sellerFlow() {
    const groups = calibration
      ? [[calibrationSeller]]
      : buildOrderableSellerGroups(accountModel.orderSellerAccounts, sellerVuLimit);
    const slotIndex = sellerSlotIndex(exec.scenario.name, groups.length);
    const group = slotIndex === null ? groupForVu(groups) : groups[slotIndex];
    const seller = nextSeller(group, sellerCursor);
    sellerCursor += 1;
    const session = loadtestSession(seller);
    sellerHandleOrder(session, seller.storeId);
    metrics.sellerIterations.add(1);
  }

  function stockFlow() {
    const group = calibration
      ? [accountModel.stockSellerAccounts[0]]
      : groupForVu(accountModel.stockGroups);
    const seller = nextSeller(group, stockCursor);
    stockCursor += 1;
    const session = loadtestSession(seller);
    sellerAdjustStock(session, exec.scenario.iterationInTest + 1);
    metrics.stockIterations.add(1);
  }

  return { browseFlow, purchaseFlow, sellerFlow, stockFlow };
}
