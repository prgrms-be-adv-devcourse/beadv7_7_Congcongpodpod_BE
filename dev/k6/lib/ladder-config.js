// 처리량 한계를 찾는 실행의 부하 모양을 만든다.
//
// 기존 stress-recovery.js는 네 흐름을 함께 올렸다. 그러면 서버가 꺾였을 때
// 주문 때문인지 조회 때문인지 구분할 수 없다. 여기서는 조회·판매자·재고를 고정하고
// 주문 도착률만 올린다. 그래야 "주문 분당 N건이 한계"라고 말할 수 있다.
//
// 한 번 실행 = 계단 한 칸이다. ORDER_RATE를 바꿔가며 여러 번 실행하고,
// 실행마다 남는 요약 파일을 나란히 비교한다. 한 칸에서 서버가 무너져도
// 앞 칸의 결과는 이미 파일로 남아 있다.

const BACKGROUND_VUS = { browse: 14, seller: 5, stock: 1 };

// 구매 계정은 seller001~150 이므로 구매 VU가 150을 넘으면 같은 계정을 두 VU가
// 동시에 쓰게 되고 refresh token이 서로 덮어쓴다.
const MAX_PURCHASE_VUS = 150;

// 1 VU 보정에서 관측한 구매 반복시간(초). maxVUs 산정에만 쓰는 어림값이다.
const ASSUMED_PURCHASE_ITERATION_SECONDS = 14;

function fail(message) {
  throw new Error(`계단 실행 설정 실패: ${message}`);
}

function requirePositive(name, value) {
  if (!Number.isFinite(value) || value <= 0) {
    fail(`${name}은 0보다 커야 합니다: ${value}`);
  }
  return value;
}

export function ladderSettingsFromEnv() {
  const orderRate = requirePositive('ORDER_RATE', Number(__ENV.ORDER_RATE));
  const holdMinutes = requirePositive('ORDER_HOLD_MINUTES', Number(__ENV.ORDER_HOLD_MINUTES || 10));
  const warmupMinutes = requirePositive(
    'ORDER_WARMUP_MINUTES',
    Number(__ENV.ORDER_WARMUP_MINUTES || 2),
  );
  const backgroundScale = requirePositive(
    'BACKGROUND_SCALE',
    Number(__ENV.BACKGROUND_SCALE || 1),
  );

  return { orderRate, holdMinutes, warmupMinutes, backgroundScale };
}

// 도착률을 맞추려면 반복시간만큼 VU가 필요하다. 서버가 느려지면 더 필요하므로
// 여유를 1.5배 둔다. 그래도 부족하면 k6가 dropped_iterations로 알려준다.
export function purchaseVuBudget(orderRate) {
  const needed = Math.ceil((orderRate * ASSUMED_PURCHASE_ITERATION_SECONDS * 1.5) / 60);
  return Math.max(5, Math.min(MAX_PURCHASE_VUS, needed));
}

export function backgroundVusFor(scale) {
  return {
    browse: Math.max(1, Math.round(BACKGROUND_VUS.browse * scale)),
    seller: Math.max(1, Math.round(BACKGROUND_VUS.seller * scale)),
    stock: Math.max(1, Math.round(BACKGROUND_VUS.stock * scale)),
  };
}

function constantVuScenario(exec, vus, totalSeconds, tags) {
  return {
    executor: 'constant-vus',
    exec,
    vus,
    duration: `${totalSeconds}s`,
    gracefulStop: '60s',
    tags,
  };
}

export function buildLadderScenarioOptions(settings) {
  const { orderRate, holdMinutes, warmupMinutes, backgroundScale } = settings;
  const background = backgroundVusFor(backgroundScale);
  const totalSeconds = Math.round((warmupMinutes + holdMinutes) * 60);
  const scenarios = {};

  scenarios.browse = constantVuScenario('browseFlow', background.browse, totalSeconds, {
    flow: 'browse',
    role: 'background',
  });

  // 판매자는 계정 하나를 VU 하나가 독점해야 해서 슬롯을 고정한다.
  // 주문이 늘어도 처리 속도는 그대로이므로 RESERVED가 쌓인다. 이는 의도한 관측 대상이다.
  for (let slotIndex = 0; slotIndex < background.seller; slotIndex += 1) {
    scenarios[`seller_${slotIndex + 1}`] = constantVuScenario('sellerFlow', 1, totalSeconds, {
      flow: 'seller',
      role: 'background',
      seller_slot: String(slotIndex),
    });
  }

  scenarios.stock = constantVuScenario('stockFlow', background.stock, totalSeconds, {
    flow: 'stock',
    role: 'background',
  });

  // 여기만 도착률 방식이다. 서버가 느려져도 목표 입력률을 유지해야
  // "입력은 이만큼인데 완료는 이만큼"이라는 비교가 가능하다.
  scenarios.purchase = {
    executor: 'ramping-arrival-rate',
    exec: 'purchaseFlow',
    startRate: 0,
    timeUnit: '1m',
    preAllocatedVUs: purchaseVuBudget(orderRate),
    maxVUs: purchaseVuBudget(orderRate),
    stages: [
      { duration: `${Math.round(warmupMinutes * 60)}s`, target: orderRate },
      { duration: `${Math.round(holdMinutes * 60)}s`, target: orderRate },
    ],
    gracefulStop: '60s',
    tags: { flow: 'purchase', role: 'ladder' },
  };

  return scenarios;
}

// 서버를 보호하려고만 둔다. 응답이 느려지는 것 자체는 우리가 찾는 결과이므로
// p95로는 중단하지 않는다. 5xx·네트워크 실패가 계속 나오면 그 칸은 실패로 본다.
export function buildLadderThresholds() {
  return {
    flow_infrastructure_failures: [
      { threshold: 'rate<0.03', abortOnFail: true, delayAbortEval: '2m' },
    ],
  };
}
