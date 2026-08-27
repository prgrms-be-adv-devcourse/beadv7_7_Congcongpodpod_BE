const KST_OFFSET_MS = 9 * 60 * 60 * 1000;

export const SLOTS_PER_WINDOW = Number(__ENV.SLOTS_PER_WINDOW || 10);

// 부하용 매장·상품은 영업시간과 픽업 창을 모두 24시간으로 통일한다.
//
// 시간대별 창을 쓰면 실행 시각에 따라 주문 가능 대상이 사라진다(2026-08-25 새벽 실행이
// 이 이유로 중단). 그런데 픽업 창만 24시간으로 바꾸면 상품 생성 자체가 거부된다 —
// Store.validatePickupTime()이 "픽업 창 ⊆ 영업시간"을 요구하기 때문이다.
// 주문 시점 검사인 Store.isOpenAt()도 영업시간을 함께 보므로 영업시간이 좁으면
// 그 시간대 밖에서는 주문이 되지 않는다.
//
// 따라서 부하용 데이터는 영업시간도 24시간으로 연다. 시간 정책 자체의 검증은
// 실제 시간 창을 쓰는 verify-flow.js의 1 VU 실행으로 분리한다.
//
// 네 유형은 이제 시간이 아니라 데이터를 네 갈래로 나누는 이름표로만 쓴다.
const ALL_DAY = {
  openTime: '00:00',
  closeTime: '23:59',
  pickupStartTime: '00:00',
  pickupEndTime: '23:59',
};

export const TIME_WINDOWS = [
  { key: 'dawn', label: '새벽', code: 'd', ...ALL_DAY },
  { key: 'morning', label: '아침', code: 'm', ...ALL_DAY },
  { key: 'afternoon', label: '오후', code: 'a', ...ALL_DAY },
  { key: 'night', label: '야간', code: 'n', ...ALL_DAY },
];

const STRESS_RATES = {
  1: { total: 110, browse: 61, purchase: 22, seller: 22, stock: 5, maxVUs: 50 },
  2: { total: 220, browse: 121, purchase: 44, seller: 44, stock: 11, maxVUs: 100 },
  3: { total: 330, browse: 182, purchase: 66, seller: 66, stock: 16, maxVUs: 150 },
  4: { total: 330, browse: 182, purchase: 66, seller: 66, stock: 16, maxVUs: 150 },
  5: { total: 330, browse: 182, purchase: 66, seller: 66, stock: 16, maxVUs: 150 },
};

function requireCampaignDate(campaignDate) {
  if (!/^\d{8}$/.test(campaignDate || '')) {
    throw new Error(`CAMPAIGN_DATE는 YYYYMMDD 형식이어야 합니다: ${campaignDate}`);
  }

  const year = Number(campaignDate.slice(0, 4));
  const month = Number(campaignDate.slice(4, 6));
  const day = Number(campaignDate.slice(6, 8));
  const parsed = new Date(Date.UTC(year, month - 1, day));
  const formatted = formatUtcDate(parsed);

  if (formatted !== campaignDate) {
    throw new Error(`CAMPAIGN_DATE가 실제 날짜가 아닙니다: ${campaignDate}`);
  }
}

function requireCampaignDay(campaignDay) {
  if (!Number.isInteger(campaignDay) || campaignDay < 1 || campaignDay > 5) {
    throw new Error(`CAMPAIGN_DAY는 1~5 정수여야 합니다: ${campaignDay}`);
  }
}

function formatUtcDate(date) {
  return `${date.getUTCFullYear()}${String(date.getUTCMonth() + 1).padStart(2, '0')}${String(
    date.getUTCDate(),
  ).padStart(2, '0')}`;
}

function shiftDate(campaignDate, offsetDays) {
  requireCampaignDate(campaignDate);
  const year = Number(campaignDate.slice(0, 4));
  const month = Number(campaignDate.slice(4, 6));
  const day = Number(campaignDate.slice(6, 8));
  return formatUtcDate(new Date(Date.UTC(year, month - 1, day + offsetDays)));
}

function minuteOfDay(value) {
  const [hour, minute] = value.split(':').map(Number);
  return hour * 60 + minute;
}

function isWithinWindow(currentMinute, startTime, endTime) {
  const startMinute = minuteOfDay(startTime);
  const endMinute = minuteOfDay(endTime);

  if (startMinute <= endMinute) {
    return currentMinute >= startMinute && currentMinute <= endMinute;
  }
  return currentMinute >= startMinute || currentMinute <= endMinute;
}

function sellerSpec(campaignDate, window, windowIndex, slot) {
  const slot2 = String(slot).padStart(2, '0');
  const key = `ld273-${campaignDate}-${window.key}-${slot2}`;
  const compactKey = `${campaignDate.slice(2)}${window.code}${slot2}`;
  const phone = `010-${campaignDate.slice(4)}-${windowIndex + 1}${slot2}`;

  return {
    key,
    windowKey: window.key,
    slot,
    email: `${key}@seed.lastdish.kr`,
    userName: `lt${compactKey}`,
    name: `부하판매자${compactKey}`,
    phone,
    store: {
      storeName: `[LOADTEST][${window.label}] ${campaignDate}-${slot2} 매장`,
      businessNumber: `7${campaignDate.slice(2)}${windowIndex + 1}${slot2}`,
      storeAddress: `서울특별시 서초구 부하테스트로 ${windowIndex + 1}-${slot}`,
      storePhone: phone,
      openTime: window.openTime,
      closeTime: window.closeTime,
      latitude: 37.4851 + windowIndex * 0.001 + slot * 0.00001,
      longitude: 127.0158 + windowIndex * 0.001 + slot * 0.00001,
      category: 'KOREAN',
      holidays: [],
    },
    dish: {
      dishName: `[LOADTEST] ${window.label} 마감팩 ${campaignDate}-${slot2}`,
      registeredAt: `${campaignDate.slice(0, 4)}-${campaignDate.slice(4, 6)}-${campaignDate.slice(6, 8)}T09:00:00`,
      description: `Issue #273 ${window.label}형 장기 운영 부하 테스트 상품`,
      category: 'KOREAN',
      stockQuantity: 1000,
      dishPrice: 20000,
      discountPrice: 14000,
      pickupStartTime: window.pickupStartTime,
      pickupEndTime: window.pickupEndTime,
    },
  };
}

export function buildDailySellerSpecs(campaignDate) {
  requireCampaignDate(campaignDate);
  if (!Number.isInteger(SLOTS_PER_WINDOW) || SLOTS_PER_WINDOW < 1) {
    throw new Error(`SLOTS_PER_WINDOW는 1 이상의 정수여야 합니다: ${SLOTS_PER_WINDOW}`);
  }

  const specs = [];
  for (let windowIndex = 0; windowIndex < TIME_WINDOWS.length; windowIndex += 1) {
    const window = TIME_WINDOWS[windowIndex];
    for (let slot = 1; slot <= SLOTS_PER_WINDOW; slot += 1) {
      specs.push(sellerSpec(campaignDate, window, windowIndex, slot));
    }
  }
  return specs;
}

export function buildTargetSellerSpecs(campaignDate, campaignDay) {
  requireCampaignDate(campaignDate);
  requireCampaignDay(campaignDay);

  const specs = [];
  for (let offset = campaignDay - 1; offset >= 0; offset -= 1) {
    specs.push(...buildDailySellerSpecs(shiftDate(campaignDate, -offset)));
  }
  return specs;
}

export function orderableWindowKeysAt(date) {
  if (!(date instanceof Date) || Number.isNaN(date.getTime())) {
    throw new Error('orderableWindowKeysAt에는 유효한 Date가 필요합니다.');
  }

  const kst = new Date(date.getTime() + KST_OFFSET_MS);
  const currentMinute = kst.getUTCHours() * 60 + kst.getUTCMinutes();
  return TIME_WINDOWS.filter((window) =>
    isWithinWindow(currentMinute, window.pickupStartTime, window.pickupEndTime),
  ).map((window) => window.key);
}

export function stressRatesForDay(campaignDay) {
  requireCampaignDay(campaignDay);
  return { ...STRESS_RATES[campaignDay] };
}
