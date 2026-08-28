import { ensureSellerLifecycle } from './lib/lifecycle.js';
import { buildTargetSellerSpecs } from './lib/long-run-config.js';

const CAMPAIGN_DATE = __ENV.CAMPAIGN_DATE;
const CAMPAIGN_DAY = Number(__ENV.CAMPAIGN_DAY);
const DATASET_EPOCH = __ENV.DATASET_EPOCH;
const RUN_ID = __ENV.RUN_ID;
const PREPARE_SHARDS = Number(__ENV.PREPARE_SHARDS || 4);
const PREPARE_SHARD_INDEX = Number(__ENV.PREPARE_SHARD_INDEX || 0);
const PREPARE_LIMIT = __ENV.PREPARE_LIMIT ? Number(__ENV.PREPARE_LIMIT) : null;

if (!RUN_ID || !/^[A-Za-z0-9._-]+$/.test(RUN_ID)) {
  throw new Error(`RUN_ID는 파일명에 안전한 값이어야 합니다: ${RUN_ID}`);
}
if (!DATASET_EPOCH) {
  throw new Error('DATASET_EPOCH 환경변수가 필요합니다.');
}
if (!Number.isInteger(PREPARE_SHARDS) || PREPARE_SHARDS < 1) {
  throw new Error(`PREPARE_SHARDS는 1 이상의 정수여야 합니다: ${PREPARE_SHARDS}`);
}
if (
  !Number.isInteger(PREPARE_SHARD_INDEX) ||
  PREPARE_SHARD_INDEX < 0 ||
  PREPARE_SHARD_INDEX >= PREPARE_SHARDS
) {
  throw new Error(
    `PREPARE_SHARD_INDEX는 0~${PREPARE_SHARDS - 1} 정수여야 합니다: ${PREPARE_SHARD_INDEX}`,
  );
}
if (PREPARE_LIMIT !== null && (!Number.isInteger(PREPARE_LIMIT) || PREPARE_LIMIT < 1)) {
  throw new Error(`PREPARE_LIMIT은 비어 있거나 1 이상의 정수여야 합니다: ${PREPARE_LIMIT}`);
}

export const options = {
  vus: 1,
  iterations: 1,
  setupTimeout: '20m',
  teardownTimeout: '1m',
  thresholds: {},
};

// 전체 목표의 앞 N개를 먼저 고른 뒤 전역 순번의 나머지로 shard 소유권을 나눈다.
export function setup() {
  const fullTarget = buildTargetSellerSpecs(CAMPAIGN_DATE, CAMPAIGN_DAY);
  const partial = PREPARE_LIMIT !== null;
  const selectedTarget = partial ? fullTarget.slice(0, PREPARE_LIMIT) : fullTarget;
  const shardSpecs = selectedTarget.filter(
    (_, globalIndex) => globalIndex % PREPARE_SHARDS === PREPARE_SHARD_INDEX,
  );
  const sellers = shardSpecs.map((spec) => ensureSellerLifecycle(spec));

  return {
    schemaVersion: 1,
    runId: RUN_ID,
    campaignDate: CAMPAIGN_DATE,
    campaignDay: CAMPAIGN_DAY,
    datasetEpoch: DATASET_EPOCH,
    reconstructedDataset: true,
    partial,
    targetCount: selectedTarget.length,
    shardCount: PREPARE_SHARDS,
    shardIndex: PREPARE_SHARD_INDEX,
    sellers,
  };
}

export default function () {
  // 실제 준비는 setup에서 한 번만 수행하고 default VU는 네트워크 요청을 만들지 않는다.
}

// 호스트 래퍼가 raw 로그의 이 한 줄만 추출해 shard manifest를 합친다.
export function teardown(data) {
  console.log(`LD273_MANIFEST=${JSON.stringify(data)}`);
}
