import { storage } from './storage';

export type InAppNotification = {
  id: string;
  title: string;
  message: string;
  createdAt: string;
  kind: 'ORDER' | 'PICKUP' | 'BENEFIT';
};

const READ_IDS_KEY = 'lastdish.notification.readIds';

// TODO: 알림 API가 추가되면 이 목록만 서버 조회로 교체합니다.
export const temporaryNotifications: InAppNotification[] = [
  { id: 'pickup-ready', title: '픽업 준비가 완료됐어요', message: '주문 내역에서 픽업 코드를 확인하고 매장을 방문해주세요.', createdAt: '방금', kind: 'PICKUP' },
  { id: 'order-confirmed', title: '매장에서 주문을 확인했어요', message: '음식을 준비하고 있어요. 픽업 준비가 끝나면 다시 알려드릴게요.', createdAt: '오늘', kind: 'ORDER' },
  { id: 'welcome-benefit', title: '라디와 함께 음식을 구조해보세요', message: '내 주변 마감 할인 음식을 발견하고 포인트와 등급을 쌓아보세요.', createdAt: '이번 주', kind: 'BENEFIT' },
];

export async function getReadNotificationIds() {
  const value = await storage.getItem(READ_IDS_KEY);
  if (!value) return new Set<string>();
  try {
    return new Set<string>(JSON.parse(value));
  } catch {
    return new Set<string>();
  }
}

export async function saveReadNotificationIds(ids: Set<string>) {
  await storage.setItem(READ_IDS_KEY, JSON.stringify([...ids]));
}
