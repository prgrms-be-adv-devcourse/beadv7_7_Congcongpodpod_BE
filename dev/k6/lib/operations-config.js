const PERIODS = [
  { minutes: 30, browse: 5, purchase: 2, seller: 2, stock: 1 },
  { minutes: 90, browse: 8, purchase: 3, seller: 3, stock: 1 },
  { minutes: 120, browse: 14, purchase: 5, seller: 5, stock: 1 },
  { minutes: 180, browse: 8, purchase: 3, seller: 3, stock: 1 },
  { minutes: 30, browse: 14, purchase: 5, seller: 5, stock: 1 },
];

const FLOW_EXEC = {
  browse: 'browseFlow',
  purchase: 'purchaseFlow',
  seller: 'sellerFlow',
  stock: 'stockFlow',
};

export const DAILY_MAX_VUS = {
  browse: 14,
  purchase: 5,
  seller: 5,
  stock: 1,
};

function fail(message) {
  throw new Error(`운영 유사 설정 실패: ${message}`);
}

function requireScale(scale) {
  if (!Number.isFinite(scale) || scale <= 0) {
    fail(`SCHEDULE_SCALE은 0보다 커야 합니다: ${scale}`);
  }
}

function stagesFor(flow, scale) {
  const stages = [];
  for (const period of PERIODS) {
    stages.push({ duration: '0s', target: period[flow] });
    stages.push({ duration: `${period.minutes * 60 * scale}s`, target: period[flow] });
  }
  return stages;
}

export function buildDailyScenarioOptions({ scheduleScale = 1, calibration = false } = {}) {
  requireScale(scheduleScale);
  const scenarios = {};
  for (const [flow, exec] of Object.entries(FLOW_EXEC)) {
    scenarios[flow] = calibration
      ? {
          executor: 'per-vu-iterations',
          exec,
          vus: 1,
          iterations: 1,
          maxDuration: '10m',
          startTime: flow === 'seller' ? '30s' : '0s',
          tags: { flow },
        }
      : {
          executor: 'ramping-vus',
          exec,
          startVUs: 0,
          stages: stagesFor(flow, scheduleScale),
          gracefulRampDown: '30s',
          gracefulStop: '30s',
          tags: { flow },
        };
  }
  return scenarios;
}

export function partitionAccountsByVu(accounts, vuCount) {
  if (!Array.isArray(accounts) || accounts.length === 0) {
    fail('분배할 계정이 없습니다.');
  }
  if (!Number.isInteger(vuCount) || vuCount < 1 || vuCount > accounts.length) {
    fail(`VU 수는 계정 수 이하여야 합니다: vus=${vuCount} accounts=${accounts.length}`);
  }

  const groups = Array.from({ length: vuCount }, () => []);
  accounts.forEach((account, index) => groups[index % vuCount].push(account));
  return groups;
}

export function selectWeightedTarget(targets, randomValue = Math.random()) {
  if (!Array.isArray(targets) || targets.length === 0) {
    fail('선택할 주문 대상이 없습니다.');
  }
  if (!Number.isFinite(randomValue) || randomValue < 0 || randomValue >= 1) {
    fail(`난수는 0 이상 1 미만이어야 합니다: ${randomValue}`);
  }

  const sorted = [...targets].sort((left, right) =>
    String(left.key || left.storeId).localeCompare(String(right.key || right.storeId)),
  );
  const popularEnd = Math.max(1, Math.ceil(sorted.length * 0.2));
  const normalEnd = Math.max(popularEnd, Math.ceil(sorted.length * 0.7));
  const popular = sorted.slice(0, popularEnd);
  const normal = sorted.slice(popularEnd, normalEnd);
  const unpopular = sorted.slice(normalEnd);

  let bucket;
  let position;
  if (randomValue < 0.6) {
    bucket = popular;
    position = randomValue / 0.6;
  } else if (randomValue < 0.95) {
    bucket = normal.length > 0 ? normal : popular;
    position = (randomValue - 0.6) / 0.35;
  } else {
    bucket = unpopular.length > 0 ? unpopular : normal.length > 0 ? normal : popular;
    position = (randomValue - 0.95) / 0.05;
  }

  return bucket[Math.min(bucket.length - 1, Math.floor(position * bucket.length))];
}
