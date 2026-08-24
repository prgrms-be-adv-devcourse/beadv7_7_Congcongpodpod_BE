import exec from 'k6/execution';

import { loginWithCredentials, refreshIfExpiring, seedCredentials } from './lib/accounts.js';
import {
  buyerBrowse,
  buyerPurchase,
  clearLeftoverCartItem,
  sellerAdjustStock,
  sellerHandleOrder,
} from './lib/flow.js';
import * as metrics from './lib/metrics.js';
import {
  DAILY_MAX_VUS,
  buildDailyScenarioOptions,
  partitionAccountsByVu,
  selectWeightedTarget,
} from './lib/operations-config.js';
import { loadRunState, orderableTargetsAt, partitionSellerPools } from './lib/run-state.js';

const CALIBRATION = __ENV.CALIBRATION === '1';
const SCHEDULE_SCALE = Number(__ENV.SCHEDULE_SCALE || 1);
const LOADTEST_PASSWORD = __ENV.LOADTEST_PASSWORD;

if (!LOADTEST_PASSWORD || LOADTEST_PASSWORD === 'change-me-before-data-creation') {
  throw new Error('LOADTEST_PASSWORD를 실제 준비용 값으로 설정해야 합니다.');
}

const runState = loadRunState(__ENV.STATE_FILE);
const sellerPools = partitionSellerPools(
  runState,
  DAILY_MAX_VUS.seller,
  DAILY_MAX_VUS.stock,
);
const orderSellerAccounts = sellerPools.orderSellers.concat(sellerPools.activitySellers);
const stockSellerAccounts = sellerPools.stockSellers;
const sellerGroups = partitionAccountsByVu(orderSellerAccounts, DAILY_MAX_VUS.seller);
const stockGroups = partitionAccountsByVu(stockSellerAccounts, DAILY_MAX_VUS.stock);
const calibrationSeller = CALIBRATION
  ? orderableTargetsAt({ sellers: orderSellerAccounts }, new Date())[0]
  : null;

export const options = {
  scenarios: buildDailyScenarioOptions({
    scheduleScale: SCHEDULE_SCALE,
    calibration: CALIBRATION,
  }),
  thresholds: {
    checks: ['rate>=0.99'],
  },
};

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

function seedSession(startAccount, accountCount, clearCartOnFirstLogin = false) {
  const accountNo = startAccount + ((__VU - 1) % accountCount);
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
  return sessionFor({ email: seller.email, password: LOADTEST_PASSWORD });
}

function currentOrderableTargets(sellers) {
  return orderableTargetsAt({ sellers }, new Date());
}

export function browseFlow() {
  const session = seedSession(151, 150);
  const target = selectWeightedTarget(currentOrderableTargets(runState.sellers));
  buyerBrowse(session, target);
  metrics.browseIterations.add(1);
}

export function purchaseFlow() {
  const session = seedSession(1, 150, true);
  const candidates = CALIBRATION
    ? [calibrationSeller]
    : currentOrderableTargets(orderSellerAccounts);
  const target = selectWeightedTarget(candidates);
  buyerPurchase(session, target);
  metrics.purchaseIterations.add(1);
}

export function sellerFlow() {
  const group = CALIBRATION ? [calibrationSeller] : groupForVu(sellerGroups);
  const seller = nextSeller(group, sellerCursor);
  sellerCursor += 1;
  const session = loadtestSession(seller);
  sellerHandleOrder(session, seller.storeId);
  metrics.sellerIterations.add(1);
}

export function stockFlow() {
  const group = CALIBRATION ? [stockSellerAccounts[0]] : groupForVu(stockGroups);
  const seller = nextSeller(group, stockCursor);
  stockCursor += 1;
  const session = loadtestSession(seller);
  sellerAdjustStock(session, exec.scenario.iterationInTest + 1);
  metrics.stockIterations.add(1);
}
