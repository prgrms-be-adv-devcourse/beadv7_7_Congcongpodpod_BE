const PERIODS = [
  { minutes: 30, browse: 5, purchase: 2, seller: 2, stock: 1 },
  { minutes: 90, browse: 8, purchase: 3, seller: 3, stock: 1 },
  { minutes: 120, browse: 14, purchase: 5, seller: 5, stock: 1 },
  { minutes: 180, browse: 8, purchase: 3, seller: 3, stock: 1 },
  { minutes: 30, browse: 14, purchase: 5, seller: 5, stock: 1 },
];

const TOTAL_SCHEDULE_MINUTES = PERIODS.reduce((sum, period) => sum + period.minutes, 0);

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

function remainingPeriodsAfter(offsetMinutes) {
  if (
    !Number.isInteger(offsetMinutes) ||
    offsetMinutes < 0 ||
    offsetMinutes >= TOTAL_SCHEDULE_MINUTES
  ) {
    fail(
      `SCHEDULE_OFFSET_MINUTES는 0~${TOTAL_SCHEDULE_MINUTES - 1} 정수여야 합니다: ${offsetMinutes}`,
    );
  }

  let remainingOffset = offsetMinutes;
  const remaining = [];
  for (const period of PERIODS) {
    if (remainingOffset >= period.minutes) {
      remainingOffset -= period.minutes;
      continue;
    }

    remaining.push({ ...period, minutes: period.minutes - remainingOffset });
    remainingOffset = 0;
  }
  return remaining;
}

function stagesFor(flow, scale, offsetMinutes) {
  const stages = [];
  for (const period of remainingPeriodsAfter(offsetMinutes)) {
    stages.push({ duration: '0s', target: period[flow] });
    stages.push({ duration: `${period.minutes * 60 * scale}s`, target: period[flow] });
  }
  return stages;
}

function sellerSlotStages(slotIndex, scale, offsetMinutes) {
  const stages = [];
  for (const period of remainingPeriodsAfter(offsetMinutes)) {
    const target = period.seller > slotIndex ? 1 : 0;
    stages.push({ duration: '0s', target });
    stages.push({ duration: `${period.minutes * 60 * scale}s`, target });
  }
  return stages;
}

export function buildDailyScenarioOptions({
  scheduleScale = 1,
  scheduleOffsetMinutes = 0,
  calibration = false,
} = {}) {
  requireScale(scheduleScale);
  remainingPeriodsAfter(scheduleOffsetMinutes);
  const scenarios = {};
  for (const [flow, exec] of Object.entries(FLOW_EXEC)) {
    if (flow === 'seller' && !calibration) {
      for (let slotIndex = 0; slotIndex < DAILY_MAX_VUS.seller; slotIndex += 1) {
        scenarios[`seller_${slotIndex + 1}`] = {
          executor: 'ramping-vus',
          exec,
          startVUs: 0,
          stages: sellerSlotStages(slotIndex, scheduleScale, scheduleOffsetMinutes),
          gracefulRampDown: '30s',
          gracefulStop: '30s',
          tags: { flow, seller_slot: String(slotIndex) },
        };
      }
      continue;
    }

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
          stages: stagesFor(flow, scheduleScale, scheduleOffsetMinutes),
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

// 주문 대상 분포. 집중도만 독립변수로 두는 A/B 실험에 쓴다.
// - weighted: 인기 20%에 주문 60%가 몰리는 기존 분포. 락 경합을 일으키는 쪽.
// - uniform:  모든 대상이 같은 확률. 경합을 최소화한 비교군.
// 처리량 한계를 잴 때는 uniform으로 재야 "락이 아니라 시스템 용량"을 재게 된다.
export const TARGET_DISTRIBUTIONS = ['weighted', 'uniform'];

export function targetDistributionFromEnv() {
  const value = __ENV.TARGET_DISTRIBUTION || 'weighted';
  if (!TARGET_DISTRIBUTIONS.includes(value)) {
    fail(`TARGET_DISTRIBUTION은 ${TARGET_DISTRIBUTIONS.join('|')} 중 하나여야 합니다: ${value}`);
  }
  return value;
}

export function selectWeightedTarget(
  targets,
  randomValue = Math.random(),
  distribution = 'weighted',
) {
  if (!Array.isArray(targets) || targets.length === 0) {
    fail('선택할 주문 대상이 없습니다.');
  }
  if (!Number.isFinite(randomValue) || randomValue < 0 || randomValue >= 1) {
    fail(`난수는 0 이상 1 미만이어야 합니다: ${randomValue}`);
  }
  if (!TARGET_DISTRIBUTIONS.includes(distribution)) {
    fail(`알 수 없는 주문 대상 분포입니다: ${distribution}`);
  }

  const sorted = [...targets].sort((left, right) =>
    String(left.key || left.storeId).localeCompare(String(right.key || right.storeId)),
  );

  if (distribution === 'uniform') {
    return sorted[Math.min(sorted.length - 1, Math.floor(randomValue * sorted.length))];
  }

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
