import { check } from 'k6';

import {
  decodeJwtExpirationMs,
  refreshIfExpiring,
  seedCredentials,
} from './lib/accounts.js';
import {
  dataOf,
  depositBalanceOf,
  errorCodeOf,
  expectedBusinessOutcomeOf,
  infrastructureFailureOf,
} from './lib/api.js';
import {
  TIME_WINDOWS,
  buildDailySellerSpecs,
  buildTargetSellerSpecs,
  orderableWindowKeysAt,
  stressRatesForDay,
} from './lib/long-run-config.js';
import {
  buildLifecyclePayloads,
  canReadOwnedStore,
  validateLifecycleResult,
} from './lib/lifecycle.js';
import { selectOldestNewReservedOrder } from './lib/order-selection.js';
import {
  buildDailyScenarioOptions,
  partitionAccountsByVu,
  selectWeightedTarget,
} from './lib/operations-config.js';
import * as operationsRuntime from './lib/operations-runtime.js';
import {
  buildStressScenarioOptions,
  buildStressThresholds,
  stressVuLimitsForDay,
} from './lib/stress-config.js';
import {
  orderableTargetsAt,
  parseRunState,
  partitionSellerPools,
  purchaseTargetOf,
  validateRunState,
} from './lib/run-state.js';

export const options = {
  vus: 1,
  iterations: 1,
  thresholds: {
    checks: ['rate==1'],
  },
};

function uniqueCount(rows, selector) {
  return new Set(rows.map(selector)).size;
}

function kstDateAtMinute(minute) {
  const kstMidnightUtc = Date.UTC(2026, 7, 23, 15, 0, 0);
  return new Date(kstMidnightUtc + minute * 60 * 1000);
}

function throwsContaining(action, expectedMessage) {
  try {
    action();
    return false;
  } catch (error) {
    return String(error.message).includes(expectedMessage);
  }
}

function manifestSeller(spec, index) {
  return {
    key: spec.key,
    windowKey: spec.windowKey,
    slot: spec.slot,
    email: spec.email,
    memberId: 6000 + index,
    storeId: 7000 + index,
    dishId: 8000 + index,
    openTime: spec.store.openTime,
    closeTime: spec.store.closeTime,
    pickupStartTime: spec.dish.pickupStartTime,
    pickupEndTime: spec.dish.pickupEndTime,
  };
}

function copy(value) {
  return JSON.parse(JSON.stringify(value));
}

export default function () {
  const daily = buildDailySellerSpecs('20260824');
  const target = buildTargetSellerSpecs('20260828', 5);
  const jwtWithOneHourExpiration = 'eyJhbGciOiJub25lIn0.eyJleHAiOjM2MDB9.';
  const credentials = seedCredentials(1);
  const stableSession = { accessTokenExpiresAtMs: 120000 };
  const lifecycleSpec = daily[0];
  const lifecyclePayloads = buildLifecyclePayloads(
    lifecycleSpec,
    'loadtest-password',
    7001,
    null,
  );
  const lifecycleResult = validateLifecycleResult(
    lifecycleSpec,
    {
      id: 6001,
      email: lifecycleSpec.email,
      userName: lifecycleSpec.userName,
      role: 'SELLER',
    },
    {
      storeId: 7001,
      memberId: 6001,
      storeName: lifecycleSpec.store.storeName,
      businessNumber: lifecycleSpec.store.businessNumber,
      storeAddress: lifecycleSpec.store.storeAddress,
      storePhone: lifecycleSpec.store.storePhone,
      openTime: lifecycleSpec.store.openTime,
      closeTime: lifecycleSpec.store.closeTime,
      latitude: lifecycleSpec.store.latitude,
      longitude: lifecycleSpec.store.longitude,
      category: lifecycleSpec.store.category,
      holidays: lifecycleSpec.store.holidays,
    },
    {
      dishId: 8001,
      storeId: 7001,
      dishName: lifecycleSpec.dish.dishName,
    },
  );
  const validState = {
    schemaVersion: 1,
    runId: 'ld273-20260824-a-day1',
    campaignDate: '20260824',
    campaignDay: 1,
    datasetEpoch: '20260824-a',
    reconstructedDataset: true,
    partial: false,
    shardIndexes: [0, 1, 2, 3],
    sellers: daily.map(manifestSeller),
  };
  const shardOrderedState = copy(validState);
  shardOrderedState.sellers = [0, 1, 2, 3].flatMap((shardIndex) =>
    validState.sellers.filter((_, index) => index % 4 === shardIndex),
  );
  const expectedState = { campaignDay: 1, datasetEpoch: '20260824-a' };

  check(decodeJwtExpirationMs(jwtWithOneHourExpiration), {
    'JWT exp 초를 밀리초로 변환': (value) => value === 3600000,
  });
  check(credentials, {
    '시드 계정 번호를 로그인 자격 증명으로 변환': (value) =>
      value.accountNo === 1 &&
      value.email === 'seller001@seed.lastdish.kr' &&
      value.password === __ENV.SEED_PASSWORD,
  });
  check(refreshIfExpiring(stableSession, 0), {
    '만료까지 60초 초과면 refresh 없이 같은 세션 유지': (value) => value === stableSession,
  });
  check(errorCodeOf({ body: JSON.stringify({ error: { code: 'AUTH001' } }) }), {
    'API 오류 응답에서 코드 추출': (value) => value === 'AUTH001',
  });
  check(null, {
    'DEP001은 예치금 소진으로 분류': () =>
      expectedBusinessOutcomeOf({
        status: 400,
        body: JSON.stringify({ success: false, error: { code: 'DEP001' } }),
      }) === 'deposit_exhausted',
    'D003은 재고 소진으로 분류': () =>
      expectedBusinessOutcomeOf({
        status: 409,
        body: JSON.stringify({ success: false, error: { code: 'D003' } }),
      }) === 'stock_exhausted',
    'ORD011은 주문 대상 소진으로 분류': () =>
      expectedBusinessOutcomeOf({
        status: 409,
        body: JSON.stringify({ success: false, error: { code: 'ORD011' } }),
      }) === 'target_unavailable',
    '인증 오류는 예상 업무 결과로 분류하지 않음': () =>
      expectedBusinessOutcomeOf({
        status: 401,
        body: JSON.stringify({ success: false, error: { code: 'AUTH001' } }),
      }) === null,
    '5xx는 오류 코드와 무관하게 예상 업무 결과로 분류하지 않음': () =>
      expectedBusinessOutcomeOf({
        status: 503,
        body: JSON.stringify({ success: false, error: { code: 'DEP001' } }),
      }) === null,
  });
  check(
    dataOf({
      body: JSON.stringify({ success: false, error: { code: 'G002' } }),
    }),
    {
      'ApiResponse 오류 응답은 데이터가 아님': (value) => value === null,
    },
  );
  check(
    depositBalanceOf({
      body: JSON.stringify({ memberId: 1, balance: 1000000000000 }),
    }),
    {
      '봉투 없는 예치금 응답에서 잔액 추출': (value) => value === 1000000000000,
    },
  );
  check(
    depositBalanceOf({
      body: JSON.stringify({
        success: true,
        data: { memberId: 1, balance: 1000000000000 },
      }),
    }),
    {
      'ApiResponse 예치금 응답에서 잔액 추출': (value) => value === 1000000000000,
    },
  );
  check(lifecyclePayloads, {
    '생애주기 payload가 실제 DTO 필드를 모두 포함': (value) =>
      value.signup.email === lifecycleSpec.email &&
      value.store.businessNumber === lifecycleSpec.store.businessNumber &&
      value.store.holidays.length === 0 &&
      value.dish.storeId === 7001 &&
      value.dish.imageKey === null &&
      value.dish.pickupStartTime === lifecycleSpec.dish.pickupStartTime &&
      value.dish.pickupEndTime === lifecycleSpec.dish.pickupEndTime,
  });
  check(null, {
    'MEMBER는 SELLER 전용 매장 조회를 건너뜀': () => !canReadOwnedStore({ role: 'MEMBER' }),
    'SELLER는 본인 매장을 조회할 수 있음': () => canReadOwnedStore({ role: 'SELLER' }),
  });
  check(lifecycleResult, {
    '검증된 생애주기 결과만 비밀 없는 상태 항목으로 변환': (value) =>
      value.memberId === 6001 &&
      value.storeId === 7001 &&
      value.dishId === 8001 &&
      value.pickupStartTime === lifecycleSpec.dish.pickupStartTime &&
      !Object.prototype.hasOwnProperty.call(value, 'password') &&
      !Object.prototype.hasOwnProperty.call(value, 'accessToken') &&
      !Object.prototype.hasOwnProperty.call(value, 'refreshToken'),
  });
  check(null, {
    '정상 상태 파일을 검증해 그대로 반환': () => {
      try {
        return validateRunState(validState, expectedState) === validState;
      } catch (_) {
        return false;
      }
    },
    'JSON 상태 파일을 파싱하고 검증': () => {
      try {
        return parseRunState(JSON.stringify(validState), expectedState).sellers.length === 40;
      } catch (_) {
        return false;
      }
    },
    '잘못된 JSON 상태 파일 거절': () =>
      throwsContaining(() => parseRunState('{', expectedState), 'JSON'),
  });

  const wrongSchema = copy(validState);
  wrongSchema.schemaVersion = 2;
  const wrongCount = copy(validState);
  wrongCount.sellers.pop();
  const wrongCampaignDay = copy(validState);
  wrongCampaignDay.campaignDay = 2;
  const wrongDatasetEpoch = copy(validState);
  wrongDatasetEpoch.datasetEpoch = 'old-db';
  const notReconstructed = copy(validState);
  notReconstructed.reconstructedDataset = false;
  const partialState = copy(validState);
  partialState.partial = true;
  const duplicateEmail = copy(validState);
  duplicateEmail.sellers[1].email = duplicateEmail.sellers[0].email;
  const duplicateMember = copy(validState);
  duplicateMember.sellers[1].memberId = duplicateMember.sellers[0].memberId;
  const duplicateStore = copy(validState);
  duplicateStore.sellers[1].storeId = duplicateStore.sellers[0].storeId;
  const duplicateDish = copy(validState);
  duplicateDish.sellers[1].dishId = duplicateDish.sellers[0].dishId;

  check(null, {
    'schemaVersion 불일치 거절': () =>
      throwsContaining(() => validateRunState(wrongSchema, expectedState), 'schemaVersion'),
    '판매자 목표 개수 불일치 거절': () =>
      throwsContaining(() => validateRunState(wrongCount, expectedState), '판매자 목표 개수'),
    'CAMPAIGN_DAY 불일치 거절': () =>
      throwsContaining(() => validateRunState(wrongCampaignDay, expectedState), 'CAMPAIGN_DAY'),
    'DATASET_EPOCH 불일치 거절': () =>
      throwsContaining(() => validateRunState(wrongDatasetEpoch, expectedState), 'DATASET_EPOCH'),
    '재구성되지 않은 상태 거절': () =>
      throwsContaining(() => validateRunState(notReconstructed, expectedState), 'reconstructedDataset'),
    '부분 검증 상태를 장기 실행 입력으로 거절': () =>
      throwsContaining(() => validateRunState(partialState, expectedState), 'partial'),
    '중복 이메일 거절': () =>
      throwsContaining(() => validateRunState(duplicateEmail, expectedState), 'email'),
    '중복 memberId 거절': () =>
      throwsContaining(() => validateRunState(duplicateMember, expectedState), 'memberId'),
    '중복 storeId 거절': () =>
      throwsContaining(() => validateRunState(duplicateStore, expectedState), 'storeId'),
    '중복 dishId 거절': () =>
      throwsContaining(() => validateRunState(duplicateDish, expectedState), 'dishId'),
  });

  check(null, {
    '상태 판매자를 실제 매장 상품 구매 대상으로 변환': () => {
      try {
        const target = purchaseTargetOf(validState.sellers[0]);
        return target.storeId === 7000 && target.dishId === 8000;
      } catch (_) {
        return false;
      }
    },
    '서버 ID가 없는 구매 대상 거절': () =>
      throwsContaining(
        () => purchaseTargetOf({ storeId: 7000, dishId: null }),
        'storeId와 dishId',
      ),
    '24시간 픽업 정책에서는 어느 시각에도 전체 판매자가 주문 대상': () => {
      try {
        return [3, 12, 15, 22].every((hour) => {
          const targets = orderableTargetsAt(validState, kstDateAtMinute(hour * 60));
          return targets.length === validState.sellers.length;
        });
      } catch (_) {
        return false;
      }
    },
    '알 수 없는 시간대 유형만 있으면 거절': () => {
      const noTargetState = copy(validState);
      noTargetState.sellers = noTargetState.sellers.map((seller) => ({
        ...seller,
        windowKey: 'unknown',
      }));
      return throwsContaining(
        () => orderableTargetsAt(noTargetState, kstDateAtMinute(15 * 60)),
        '주문 가능 대상',
      );
    },
    '판매자 주문 처리 풀과 재고 풀을 겹치지 않게 분리': () => {
      try {
        const pools = partitionSellerPools(validState, 5, 2);
        const orderEmails = new Set(pools.orderSellers.map((seller) => seller.email));
        return (
          pools.orderSellers.length === 5 &&
          pools.stockSellers.length === 2 &&
          pools.activitySellers.length === 33 &&
          pools.stockSellers.every((seller) => !orderEmails.has(seller.email))
        );
      } catch (_) {
        return false;
      }
    },
    '판매자 풀 필요 계정이 상태보다 많으면 거절': () =>
      throwsContaining(() => partitionSellerPools(validState, 30, 11), '판매자 계정'),
  });

  check(null, {
    '운영 유사 시간표는 판매자 전용 슬롯을 포함한 여덟 ramping-vus 시나리오로 구성': () => {
      try {
        const scenarios = buildDailyScenarioOptions({ scheduleScale: 1, calibration: false });
        const sellerScenarios = Array.from({ length: 5 }, (_, index) =>
          scenarios[`seller_${index + 1}`],
        );
        return (
          Object.keys(scenarios).length === 8 &&
          Object.values(scenarios).every((scenario) => scenario.executor === 'ramping-vus') &&
          sellerScenarios.every(
            (scenario, index) =>
              scenario &&
              scenario.exec === 'sellerFlow' &&
              scenario.tags.seller_slot === String(index),
          )
        );
      } catch (_) {
        return false;
      }
    },
    '조회 시간표는 즉시 전환하며 09시30분부터 450분 유지': () => {
      try {
        const stages = buildDailyScenarioOptions({ scheduleScale: 1, calibration: false }).browse
          .stages;
        const holds = stages.filter((stage) => stage.duration !== '0s');
        return (
          JSON.stringify(holds.map((stage) => stage.target)) === JSON.stringify([5, 8, 14, 8, 14]) &&
          holds.reduce((sum, stage) => sum + Number(stage.duration.replace('s', '')), 0) === 27000 &&
          stages.every((stage, index) => index % 2 === 1 || stage.duration === '0s')
        );
      } catch (_) {
        return false;
      }
    },
    '단축 시간표는 전체 유지시간을 27초로 축소': () => {
      try {
        const stages = buildDailyScenarioOptions({ scheduleScale: 0.001, calibration: false }).purchase
          .stages;
        const totalSeconds = stages
          .filter((stage) => stage.duration !== '0s')
          .reduce((sum, stage) => sum + Number(stage.duration.replace('s', '')), 0);
        return Math.abs(totalSeconds - 27) < 0.000001;
      } catch (_) {
        return false;
      }
    },
    '12시38분 재개는 현재 구간의 남은 52분과 17시까지만 실행': () => {
      try {
        const stages = buildDailyScenarioOptions({
          scheduleScale: 1,
          scheduleOffsetMinutes: 188,
          calibration: false,
        }).browse.stages;
        const holds = stages.filter((stage) => stage.duration !== '0s');
        return (
          JSON.stringify(holds.map((stage) => stage.target)) === JSON.stringify([14, 8, 14]) &&
          JSON.stringify(holds.map((stage) => stage.duration)) ===
            JSON.stringify(['3120s', '10800s', '1800s'])
        );
      } catch (_) {
        return false;
      }
    },
    '운영 시간표 450분을 모두 지난 후 재개는 거절': () =>
      throwsContaining(
        () =>
          buildDailyScenarioOptions({
            scheduleScale: 1,
            scheduleOffsetMinutes: 450,
            calibration: false,
          }),
        'SCHEDULE_OFFSET_MINUTES',
      ),
    '캘리브레이션은 네 흐름을 각각 1 VU 1회 실행': () => {
      try {
        const scenarios = buildDailyScenarioOptions({ scheduleScale: 1, calibration: true });
        return Object.values(scenarios).every(
          (scenario) =>
            scenario.executor === 'per-vu-iterations' && scenario.vus === 1 && scenario.iterations === 1,
        );
      } catch (_) {
        return false;
      }
    },
    '캘리브레이션 판매자는 구매 주문 생성 뒤 시작': () => {
      try {
        const scenarios = buildDailyScenarioOptions({ scheduleScale: 1, calibration: true });
        return scenarios.purchase.startTime === '0s' && scenarios.seller.startTime === '30s';
      } catch (_) {
        return false;
      }
    },
  });

  check(null, {
    '판매자 실행 풀은 24시간 정책에서 네 유형을 모두 유지': () => {
      try {
        const candidates = operationsRuntime.orderableSellerCandidates(
          validState.sellers,
          kstDateAtMinute(12 * 60 + 38),
        );
        const windowKeys = new Set(candidates.map((seller) => seller.windowKey));
        return candidates.length === validState.sellers.length && windowKeys.size === 4;
      } catch (_) {
        return false;
      }
    },
    '샤드 순서 상태도 주문 가능한 판매자를 빠짐없이 VU별 분배': () => {
      try {
        if (typeof operationsRuntime.buildOrderableSellerGroups !== 'function') {
          return false;
        }
        const accountModel = operationsRuntime.buildOperationsAccountModel(shardOrderedState, {
          sellerVuLimit: 5,
          stockVuLimit: 1,
        });
        const groups = operationsRuntime.buildOrderableSellerGroups(
          accountModel.orderSellerAccounts,
          5,
          kstDateAtMinute(12 * 60 + 38),
        );
        const assigned = groups.reduce((sum, group) => sum + group.length, 0);
        return (
          groups.length === 5 &&
          groups.every((group) => group.length > 0) &&
          assigned === accountModel.orderSellerAccounts.length
        );
      } catch (_) {
        return false;
      }
    },
    '판매자 슬롯 시나리오는 VU 번호와 무관하게 고정 그룹을 선택': () => {
      try {
        return (
          typeof operationsRuntime.sellerSlotIndex === 'function' &&
          operationsRuntime.sellerSlotIndex('seller_1', 5) === 0 &&
          operationsRuntime.sellerSlotIndex('seller_5', 5) === 4
        );
      } catch (_) {
        return false;
      }
    },
  });

  const weightedTargets = Array.from({ length: 10 }, (_, index) => ({
    key: `target-${String(index + 1).padStart(2, '0')}`,
    storeId: index + 1,
    dishId: index + 101,
  }));
  check(null, {
    '인기 20퍼센트는 난수 0부터 0.60 구간에서 선택': () => {
      try {
        return (
          selectWeightedTarget(weightedTargets, 0).storeId === 1 &&
          selectWeightedTarget(weightedTargets, 0.599999).storeId === 2
        );
      } catch (_) {
        return false;
      }
    },
    '일반 50퍼센트는 난수 0.60부터 0.95 구간에서 선택': () => {
      try {
        return (
          selectWeightedTarget(weightedTargets, 0.6).storeId === 3 &&
          selectWeightedTarget(weightedTargets, 0.949999).storeId === 7
        );
      } catch (_) {
        return false;
      }
    },
    '비인기 30퍼센트는 난수 0.95부터 1 구간에서 선택': () => {
      try {
        return (
          selectWeightedTarget(weightedTargets, 0.95).storeId === 8 &&
          selectWeightedTarget(weightedTargets, 0.999999).storeId === 10
        );
      } catch (_) {
        return false;
      }
    },
    'uniform 분포는 대상 10개를 같은 폭으로 나눠 선택': () => {
      try {
        return (
          selectWeightedTarget(weightedTargets, 0, 'uniform').storeId === 1 &&
          selectWeightedTarget(weightedTargets, 0.05, 'uniform').storeId === 1 &&
          selectWeightedTarget(weightedTargets, 0.15, 'uniform').storeId === 2 &&
          selectWeightedTarget(weightedTargets, 0.55, 'uniform').storeId === 6 &&
          selectWeightedTarget(weightedTargets, 0.999999, 'uniform').storeId === 10
        );
      } catch (_) {
        return false;
      }
    },
    'uniform 분포는 1000회 추출에서 각 대상이 10퍼센트 근처': () => {
      try {
        const counts = new Map();
        for (let index = 0; index < 1000; index += 1) {
          const picked = selectWeightedTarget(weightedTargets, index / 1000, 'uniform');
          counts.set(picked.storeId, (counts.get(picked.storeId) || 0) + 1);
        }
        return (
          counts.size === 10 &&
          [...counts.values()].every((count) => count >= 90 && count <= 110)
        );
      } catch (_) {
        return false;
      }
    },
    'weighted 분포는 같은 추출에서 인기 대상에 60퍼센트가 몰림': () => {
      try {
        let popular = 0;
        for (let index = 0; index < 1000; index += 1) {
          const picked = selectWeightedTarget(weightedTargets, index / 1000, 'weighted');
          if (picked.storeId <= 2) {
            popular += 1;
          }
        }
        return popular >= 590 && popular <= 610;
      } catch (_) {
        return false;
      }
    },
    '알 수 없는 분포 이름은 거절': () =>
      throwsContaining(
        () => selectWeightedTarget(weightedTargets, 0.5, 'random'),
        '주문 대상 분포',
      ),
    '난수 범위를 벗어나면 대상 선택 거절': () =>
      throwsContaining(() => selectWeightedTarget(weightedTargets, 1), '난수'),
  });

  check(null, {
    '판매자 계정을 VU별 겹치지 않는 묶음으로 분배': () => {
      try {
        const groups = partitionAccountsByVu(weightedTargets, 3);
        const flattened = groups.flat();
        return (
          JSON.stringify(groups.map((group) => group.length)) === JSON.stringify([4, 3, 3]) &&
          new Set(flattened.map((target) => target.storeId)).size === 10
        );
      } catch (_) {
        return false;
      }
    },
    '계정보다 많은 VU 묶음은 거절': () =>
      throwsContaining(() => partitionAccountsByVu(weightedTargets, 11), '계정'),
  });
  check(null, {
    '회원 역할이 SELLER가 아니면 생애주기 결과 거절': () =>
      throwsContaining(
        () =>
          validateLifecycleResult(
            lifecycleSpec,
            {
              id: 6001,
              email: lifecycleSpec.email,
              userName: lifecycleSpec.userName,
              role: 'MEMBER',
            },
            { storeId: 7001, memberId: 6001 },
            { dishId: 8001, storeId: 7001, dishName: lifecycleSpec.dish.dishName },
          ),
        'SELLER',
      ),
  });
  check(null, {
    '매장 주소가 결정적 spec과 다르면 생애주기 결과 거절': () =>
      throwsContaining(
        () =>
          validateLifecycleResult(
            lifecycleSpec,
            {
              id: 6001,
              email: lifecycleSpec.email,
              userName: lifecycleSpec.userName,
              role: 'SELLER',
            },
            {
              storeId: 7001,
              memberId: 6001,
              storeName: lifecycleSpec.store.storeName,
              businessNumber: lifecycleSpec.store.businessNumber,
              storeAddress: '다른 주소',
              storePhone: lifecycleSpec.store.storePhone,
              openTime: lifecycleSpec.store.openTime,
              closeTime: lifecycleSpec.store.closeTime,
              latitude: lifecycleSpec.store.latitude,
              longitude: lifecycleSpec.store.longitude,
              category: lifecycleSpec.store.category,
              holidays: lifecycleSpec.store.holidays,
            },
            { dishId: 8001, storeId: 7001, dishName: lifecycleSpec.dish.dishName },
          ),
        '매장 식별자',
      ),
  });

  check(daily, {
    '일일 판매자 40개': (rows) => rows.length === 40,
    '유형별 판매자 10개': (rows) =>
      TIME_WINDOWS.every(
        (window) => rows.filter((row) => row.windowKey === window.key).length === 10,
      ),
    '일일 공통 키 고유': (rows) => uniqueCount(rows, (row) => row.key) === 40,
    '일일 이메일 고유': (rows) => uniqueCount(rows, (row) => row.email) === 40,
    '일일 사용자명 고유': (rows) => uniqueCount(rows, (row) => row.userName) === 40,
    '일일 전화번호 고유': (rows) => uniqueCount(rows, (row) => row.phone) === 40,
    '일일 사업자번호 고유': (rows) =>
      uniqueCount(rows, (row) => row.store.businessNumber) === 40,
    '사용자명 4~20자': (rows) =>
      rows.every((row) => row.userName.length >= 4 && row.userName.length <= 20),
    '비밀번호를 설계도에 저장하지 않음': (rows) =>
      rows.every(
        (row) =>
          !Object.prototype.hasOwnProperty.call(row, 'password') &&
          !Object.prototype.hasOwnProperty.call(row, 'accessToken') &&
          !Object.prototype.hasOwnProperty.call(row, 'refreshToken'),
      ),
  });

  check(target, {
    '5일차 목표 판매자 200개': (rows) => rows.length === 200,
    '5일차 목표 공통 키 고유': (rows) => uniqueCount(rows, (row) => row.key) === 200,
    '5일차 목표 이메일 고유': (rows) => uniqueCount(rows, (row) => row.email) === 200,
    '5일차 목표 사용자명 고유': (rows) => uniqueCount(rows, (row) => row.userName) === 200,
    '5일차 목표 전화번호 고유': (rows) => uniqueCount(rows, (row) => row.phone) === 200,
  });

  const selectedOrder = selectOldestNewReservedOrder(
    [
      { orderId: 750005, status: 'RESERVED', phone: '010-A' },
      { orderId: 750002, status: 'RESERVED', phone: '010-B' },
      { orderId: 750001, status: 'PICKUP_READY', phone: '010-C' },
      { orderId: 700000, status: 'RESERVED', phone: '010-D' },
    ],
    750000,
  );
  const noSelectedOrder = selectOldestNewReservedOrder(
    [
      { orderId: 750000, status: 'RESERVED' },
      { orderId: 750001, status: 'PICKUP_READY' },
    ],
    750000,
  );

  check(selectedOrder, {
    '전화번호와 무관하게 가장 오래된 새 RESERVED 선택': (order) =>
      order && order.orderId === 750002,
  });
  check(noSelectedOrder, {
    '새 RESERVED 주문이 없으면 null': (order) => order === null,
  });

  for (let minute = 0; minute < 24 * 60; minute += 30) {
    check(orderableWindowKeysAt(kstDateAtMinute(minute)), {
      [`KST ${String(Math.floor(minute / 60)).padStart(2, '0')}:${String(minute % 60).padStart(2, '0')} 주문 가능 유형 존재`]:
        (keys) => keys.length >= 1,
    });
  }

  for (let campaignDay = 1; campaignDay <= 5; campaignDay += 1) {
    const rates = stressRatesForDay(campaignDay);
    check(rates, {
      [`${campaignDay}일차 스트레스 흐름 합계 일치`]: (value) =>
        value.browse + value.purchase + value.seller + value.stock === value.total,
    });
  }

  check(null, {
    '스트레스 실행은 네 ramping-arrival-rate 시나리오로 구성': () => {
      const scenarios = buildStressScenarioOptions(1);
      return (
        Object.keys(scenarios).length === 4 &&
        Object.values(scenarios).every(
          (scenario) =>
            scenario.executor === 'ramping-arrival-rate' &&
            scenario.timeUnit === '1m' &&
            scenario.tags.phase === 'stress_recovery',
        )
      );
    },
    '1일차 흐름별 상한과 회복 도착률이 설계값과 일치': () => {
      const scenarios = buildStressScenarioOptions(1);
      return (
        JSON.stringify(Object.values(scenarios).map((scenario) => scenario.stages[0].target)) ===
          JSON.stringify([61, 22, 22, 5]) &&
        JSON.stringify(Object.values(scenarios).map((scenario) => scenario.stages[2].target)) ===
          JSON.stringify([12, 4, 4, 2])
      );
    },
    '스트레스는 즉시 회복 전환과 재급증 뒤 40분에 신규 반복을 중단': () => {
      const scenarios = buildStressScenarioOptions(1);
      return Object.values(scenarios).every(
        (scenario) =>
          JSON.stringify(scenario.stages.map((stage) => stage.duration)) ===
            JSON.stringify(['5m', '10m', '0s', '10m', '0s', '3m', '0s', '12m']) &&
          JSON.stringify(scenario.stages.map((stage) => stage.target)) ===
            JSON.stringify([
              scenario.stages[0].target,
              scenario.stages[0].target,
              scenario.stages[2].target,
              scenario.stages[2].target,
              scenario.stages[0].target,
              scenario.stages[0].target,
              scenario.stages[2].target,
              scenario.stages[2].target,
            ]) &&
          scenario.gracefulStop === '10m',
      );
    },
    '3일차 흐름별 maxVUs 합계는 전체 상한 150': () => {
      const scenarios = buildStressScenarioOptions(3);
      const maxVUs = Object.values(scenarios).map((scenario) => scenario.maxVUs);
      return (
        JSON.stringify(maxVUs) === JSON.stringify([83, 30, 30, 7]) &&
        maxVUs.reduce((sum, value) => sum + value, 0) === 150
      );
    },
    '1일차 스트레스 판매자와 재고 VU는 서로 다른 계정을 배정 가능': () => {
      const limits = stressVuLimitsForDay(1);
      const pools = partitionSellerPools(validState, limits.seller, limits.stock);
      const sellerGroups = partitionAccountsByVu(
        pools.orderSellers.concat(pools.activitySellers),
        limits.seller,
      );
      const stockEmails = new Set(pools.stockSellers.map((seller) => seller.email));
      return (
        sellerGroups.length === 10 &&
        pools.stockSellers.length === 2 &&
        sellerGroups.flat().every((seller) => !stockEmails.has(seller.email))
      );
    },
    '1일차 스트레스 계정 모델은 판매자 10개와 재고 2개 VU 묶음을 생성': () => {
      const model = operationsRuntime.buildOperationsAccountModel(validState, {
        sellerVuLimit: 10,
        stockVuLimit: 2,
      });
      const stockEmails = new Set(model.stockSellerAccounts.map((seller) => seller.email));
      return (
        model.sellerGroups.length === 10 &&
        model.stockGroups.length === 2 &&
        model.sellerGroups.flat().every((seller) => !stockEmails.has(seller.email))
      );
    },
    '5xx와 네트워크 실패만 인프라 실패로 분류': () =>
      infrastructureFailureOf({ status: 503 }) &&
      infrastructureFailureOf({ status: 0 }) &&
      !infrastructureFailureOf({ status: 429 }) &&
      !infrastructureFailureOf({ status: 400 }),
    '유휴 p95의 3배를 스트레스 보호 중단 기준으로 사용': () => {
      const thresholds = buildStressThresholds(250);
      return (
        thresholds.http_req_duration[0].threshold === 'p(95)<750' &&
        thresholds.flow_infrastructure_failures[0].threshold === 'rate<0.03' &&
        thresholds.flow_order_create_success[0].threshold === 'rate>=0.95'
      );
    },
    '유휴 p95가 없으면 스트레스 임계값 생성을 거절': () =>
      throwsContaining(() => buildStressThresholds(0), 'BASELINE_P95_MS'),
  });

  console.log('순수 모델 검증 완료: HTTP 요청 0회');
}
