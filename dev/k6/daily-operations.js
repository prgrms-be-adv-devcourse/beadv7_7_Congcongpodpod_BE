import crypto from 'k6/crypto';

import { DAILY_MAX_VUS, buildDailyScenarioOptions } from './lib/operations-config.js';
import { createOperationsRuntime } from './lib/operations-runtime.js';

const CALIBRATION = __ENV.CALIBRATION === '1';
const SCHEDULE_SCALE = Number(__ENV.SCHEDULE_SCALE || 1);
const SCHEDULE_OFFSET_MINUTES = Number(__ENV.SCHEDULE_OFFSET_MINUTES || 0);
const CAMPAIGN_DAY = Number(__ENV.CAMPAIGN_DAY);
const RUN_ID = __ENV.RUN_ID;
const DATASET_EPOCH = __ENV.DATASET_EPOCH;
const STATE_FILE = __ENV.STATE_FILE;

if (!RUN_ID) {
  throw new Error('일일 운영 유사 실행을 식별할 RUN_ID가 필요합니다.');
}

const stateFileSha256 = crypto.sha256(open(STATE_FILE), 'hex');
const runtime = createOperationsRuntime({
  sellerVuLimit: DAILY_MAX_VUS.seller,
  stockVuLimit: DAILY_MAX_VUS.stock,
  calibration: CALIBRATION,
});

export const options = {
  scenarios: buildDailyScenarioOptions({
    scheduleScale: SCHEDULE_SCALE,
    scheduleOffsetMinutes: SCHEDULE_OFFSET_MINUTES,
    calibration: CALIBRATION,
  }),
  thresholds: {
    checks: ['rate>=0.99'],
  },
  tags: {
    testid: RUN_ID,
    phase: 'daily_operations',
    campaign_day: String(CAMPAIGN_DAY),
    dataset_epoch: DATASET_EPOCH,
    schedule_offset_minutes: String(SCHEDULE_OFFSET_MINUTES),
    state_file_sha256: stateFileSha256,
  },
};

if (!CALIBRATION && SCHEDULE_OFFSET_MINUTES > 0) {
  console.info(`운영 유사 시간표를 ${SCHEDULE_OFFSET_MINUTES}분 지난 구간부터 재개합니다.`);
}

export function browseFlow() {
  runtime.browseFlow();
}

export function purchaseFlow() {
  runtime.purchaseFlow();
}

export function sellerFlow() {
  runtime.sellerFlow();
}

export function stockFlow() {
  runtime.stockFlow();
}
