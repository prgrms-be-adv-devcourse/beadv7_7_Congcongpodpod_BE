import { stressRatesForDay } from './long-run-config.js';

const FLOWS = ['browse', 'purchase', 'seller', 'stock'];

const FLOW_EXEC = {
  browse: 'browseFlow',
  purchase: 'purchaseFlow',
  seller: 'sellerFlow',
  stock: 'stockFlow',
};

const RECOVERY_RATES = {
  browse: 12,
  purchase: 4,
  seller: 4,
  stock: 2,
};

function allocateMaxVUs(rates) {
  const allocations = FLOWS.map((flow) => {
    const exact = (rates.maxVUs * rates[flow]) / rates.total;
    return { flow, value: Math.floor(exact), remainder: exact - Math.floor(exact) };
  });

  let remaining = rates.maxVUs - allocations.reduce((sum, row) => sum + row.value, 0);
  const remainderOrder = [...allocations].sort(
    (left, right) => right.remainder - left.remainder || FLOWS.indexOf(left.flow) - FLOWS.indexOf(right.flow),
  );
  for (let index = 0; index < remaining; index += 1) {
    remainderOrder[index].value += 1;
  }

  return Object.fromEntries(allocations.map((row) => [row.flow, row.value]));
}

export function stressVuLimitsForDay(campaignDay) {
  return allocateMaxVUs(stressRatesForDay(campaignDay));
}

export function buildStressScenarioOptions(campaignDay) {
  const rates = stressRatesForDay(campaignDay);
  const maxVUs = stressVuLimitsForDay(campaignDay);

  return Object.fromEntries(
    FLOWS.map((flow) => [
      flow,
      {
        executor: 'ramping-arrival-rate',
        exec: FLOW_EXEC[flow],
        startRate: 0,
        timeUnit: '1m',
        preAllocatedVUs: maxVUs[flow],
        maxVUs: maxVUs[flow],
        stages: [
          { duration: '5m', target: rates[flow] },
          { duration: '10m', target: rates[flow] },
          { duration: '10m', target: RECOVERY_RATES[flow] },
          { duration: '3m', target: rates[flow] },
          { duration: '12m', target: RECOVERY_RATES[flow] },
        ],
        gracefulStop: '10m',
        tags: { flow, phase: 'stress_recovery' },
      },
    ]),
  );
}

export function buildStressThresholds(baselineP95Ms) {
  if (!Number.isFinite(baselineP95Ms) || baselineP95Ms <= 0) {
    throw new Error(`BASELINE_P95_MS는 0보다 커야 합니다: ${baselineP95Ms}`);
  }

  return {
    checks: ['rate>=0.95'],
    flow_infrastructure_failures: [
      { threshold: 'rate<0.03', abortOnFail: true, delayAbortEval: '2m' },
    ],
    flow_order_create_success: [
      { threshold: 'rate>=0.95', abortOnFail: true, delayAbortEval: '2m' },
    ],
    http_req_duration: [
      {
        threshold: `p(95)<${baselineP95Ms * 3}`,
        abortOnFail: true,
        delayAbortEval: '3m',
      },
    ],
  };
}
