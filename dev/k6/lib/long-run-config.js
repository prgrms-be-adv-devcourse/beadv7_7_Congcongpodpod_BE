const KST_OFFSET_MS = 9 * 60 * 60 * 1000;

export const SLOTS_PER_WINDOW = Number(__ENV.SLOTS_PER_WINDOW || 10);

export const TIME_WINDOWS = [
  {
    key: 'dawn',
    label: '새벽',
    code: 'd',
    openTime: '23:00',
    closeTime: '07:00',
    pickupStartTime: '00:00',
    pickupEndTime: '06:30',
  },
  {
    key: 'morning',
    label: '아침',
    code: 'm',
    openTime: '05:00',
    closeTime: '13:00',
    pickupStartTime: '06:00',
    pickupEndTime: '12:30',
  },
  {
    key: 'afternoon',
    label: '오후',
    code: 'a',
    openTime: '11:00',
    closeTime: '19:00',
    pickupStartTime: '12:00',
    pickupEndTime: '18:30',
  },
  {
    key: 'night',
    label: '야간',
    code: 'n',
    openTime: '17:00',
    closeTime: '01:00',
    pickupStartTime: '18:00',
    pickupEndTime: '00:30',
  },
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
