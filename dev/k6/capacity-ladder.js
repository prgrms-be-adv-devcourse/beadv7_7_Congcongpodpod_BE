// 주문 처리량의 한계를 찾는 실행. 계단 한 칸에 해당한다.
//
// 사용법 (ORDER_RATE를 바꿔가며 여러 번 실행):
//   ./k6.sh capacity-ladder -e ORDER_RATE=30  -e RESULT_LABEL=ladder-030
//   ./k6.sh capacity-ladder -e ORDER_RATE=60  -e RESULT_LABEL=ladder-060
//   ./k6.sh capacity-ladder -e ORDER_RATE=120 -e RESULT_LABEL=ladder-120
//   ./k6.sh capacity-ladder -e ORDER_RATE=240 -e RESULT_LABEL=ladder-240
//
// 조회·판매자·재고는 고정하고 주문 도착률만 바꾼다. 그래야 꺾인 원인을 주문으로 좁힐 수 있다.
// 주문 대상은 기본이 uniform(균등)이다. 여기서 재는 것은 락 경합이 아니라 시스템 용량이므로
// 대상을 고르게 펴서 경합을 최소화한 상태로 재야 한다.
import crypto from 'k6/crypto';

import {
  backgroundVusFor,
  buildLadderScenarioOptions,
  buildLadderThresholds,
  ladderSettingsFromEnv,
  purchaseVuBudget,
} from './lib/ladder-config.js';
import { createOperationsRuntime } from './lib/operations-runtime.js';
import { buildSummaryHandler } from './lib/summary.js';

const RUN_ID = __ENV.RUN_ID;
const DATASET_EPOCH = __ENV.DATASET_EPOCH;
const STATE_FILE = __ENV.STATE_FILE;
const CAMPAIGN_DAY = Number(__ENV.CAMPAIGN_DAY);

if (!RUN_ID) {
  throw new Error('실행을 식별할 RUN_ID가 필요합니다.');
}
if (!__ENV.RESULT_LABEL) {
  throw new Error(
    '계단 칸을 구분할 RESULT_LABEL이 필요합니다. 예: -e RESULT_LABEL=ladder-030',
  );
}

const settings = ladderSettingsFromEnv();
const background = backgroundVusFor(settings.backgroundScale);
const stateFileSha256 = crypto.sha256(open(STATE_FILE), 'hex');

const runtime = createOperationsRuntime({
  sellerVuLimit: background.seller,
  stockVuLimit: background.stock,
});

export const options = {
  scenarios: buildLadderScenarioOptions(settings),
  thresholds: buildLadderThresholds(),
  tags: {
    testid: RUN_ID,
    phase: 'capacity_ladder',
    order_rate: String(settings.orderRate),
    order_distribution: __ENV.TARGET_DISTRIBUTION || 'weighted',
    campaign_day: String(CAMPAIGN_DAY),
    dataset_epoch: DATASET_EPOCH,
    state_file_sha256: stateFileSha256,
  },
};

// 요약 파일에 이 칸의 설정을 함께 남긴다. 나중에 파일만 보고도 조건을 알 수 있어야 한다.
export const handleSummary = buildSummaryHandler({
  orderRatePerMinute: settings.orderRate,
  orderDistribution: __ENV.TARGET_DISTRIBUTION || 'weighted',
  warmupMinutes: settings.warmupMinutes,
  holdMinutes: settings.holdMinutes,
  backgroundVus: background,
  purchaseMaxVus: purchaseVuBudget(settings.orderRate),
});

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
  `계단 실행: runId=${RUN_ID} label=${__ENV.RESULT_LABEL} 주문=${settings.orderRate}건/분 ` +
    `분포=${__ENV.TARGET_DISTRIBUTION || 'weighted'} ` +
    `배경=조회${background.browse}·판매자${background.seller}·재고${background.stock} ` +
    `구매maxVUs=${purchaseVuBudget(settings.orderRate)} ` +
    `워밍업${settings.warmupMinutes}분+측정${settings.holdMinutes}분`,
);
