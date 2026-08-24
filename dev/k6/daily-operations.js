import { DAILY_MAX_VUS, buildDailyScenarioOptions } from './lib/operations-config.js';
import { createOperationsRuntime } from './lib/operations-runtime.js';

const CALIBRATION = __ENV.CALIBRATION === '1';
const SCHEDULE_SCALE = Number(__ENV.SCHEDULE_SCALE || 1);
const runtime = createOperationsRuntime({
  sellerVuLimit: DAILY_MAX_VUS.seller,
  stockVuLimit: DAILY_MAX_VUS.stock,
  calibration: CALIBRATION,
});

export const options = {
  scenarios: buildDailyScenarioOptions({
    scheduleScale: SCHEDULE_SCALE,
    calibration: CALIBRATION,
  }),
  thresholds: {
    checks: ['rate>=0.99'],
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
