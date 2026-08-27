import crypto from 'k6/crypto';

import { createOperationsRuntime } from './lib/operations-runtime.js';
import {
  buildStressScenarioOptions,
  buildStressThresholds,
  stressVuLimitsForDay,
} from './lib/stress-config.js';

const CAMPAIGN_DAY = Number(__ENV.CAMPAIGN_DAY);
const BASELINE_P95_MS = Number(__ENV.BASELINE_P95_MS);
const RUN_ID = __ENV.RUN_ID;
const DATASET_EPOCH = __ENV.DATASET_EPOCH;
const STATE_FILE = __ENV.STATE_FILE;

if (__ENV.STRESS_APPROVED !== '1') {
  throw new Error('16:50 수동 게이트 통과 후 STRESS_APPROVED=1로 실행해야 합니다.');
}
if (!RUN_ID) {
  throw new Error('운영 유사 실행과 연결할 RUN_ID가 필요합니다.');
}

const stateFileSha256 = crypto.sha256(open(STATE_FILE), 'hex');
const stressVuLimits = stressVuLimitsForDay(CAMPAIGN_DAY);
const runtime = createOperationsRuntime({
  sellerVuLimit: stressVuLimits.seller,
  stockVuLimit: stressVuLimits.stock,
});

export const options = {
  scenarios: buildStressScenarioOptions(CAMPAIGN_DAY),
  thresholds: buildStressThresholds(BASELINE_P95_MS),
  tags: {
    testid: RUN_ID,
    phase: 'stress_recovery',
    campaign_day: String(CAMPAIGN_DAY),
    dataset_epoch: DATASET_EPOCH,
    state_file_sha256: stateFileSha256,
  },
};

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

console.info(
  `스트레스·회복 설정: runId=${RUN_ID} campaignDay=${CAMPAIGN_DAY} datasetEpoch=${DATASET_EPOCH} stateSha256=${stateFileSha256}`,
);
