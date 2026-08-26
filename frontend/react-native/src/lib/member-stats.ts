import { api } from './api';
import { cachedQuery } from './query-cache';

export const temporaryMemberStats = { grade: '새싹 미식가', level: 3, points: 2450, savedAmount: 48600, completedPickupCount: 12, currentLevelStartPickupCount: 10, nextLevelPickupCount: 20 };

type Envelope<T> = { data?: T };
type ApiLevel = { dishLevel: string; purchaseCount: number; discountAmount: number; remainToNextLevel: number };
export type PointHistory = { historyId: number; orderId?: number | null; type: 'EARN' | 'USE' | 'EXPIRE' | 'REFUND'; amount: number; balanceAfter: number; expiresAt?: string | null; createdAt: string };
type PointHistoryPage = { content?: PointHistory[] };

const levelNames: Record<string, { level: number; name: string }> = {
  LEVEL_1: { level: 1, name: '양념 종지' },
  LEVEL_2: { level: 2, name: '밥그릇' },
  LEVEL_3: { level: 3, name: '뚝배기' },
  LEVEL_4: { level: 4, name: '전골냄비' },
  LEVEL_5: { level: 5, name: '가마솥' },
};

const unwrap = <T,>(value: T | Envelope<T>): T => value && typeof value === 'object' && 'data' in value && value.data !== undefined ? value.data : value as T;

export type MemberLevelStats = {
  dishLevel: string;
  level: number;
  grade: string;
  purchaseCount: number;
  savedAmount: number;
  remainToNextLevel: number;
};

export async function getMemberLevel(force = false) {
  return cachedQuery('member-level', async () => {
    const value = unwrap(await api<ApiLevel | Envelope<ApiLevel>>('/levels/info'));
    const visual = levelNames[value.dishLevel] ?? { level: 1, name: value.dishLevel || '등급 확인 중' };
    return { dishLevel: value.dishLevel, level: visual.level, grade: visual.name, purchaseCount: Number(value.purchaseCount ?? 0), savedAmount: Number(value.discountAmount ?? 0), remainToNextLevel: Number(value.remainToNextLevel ?? 0) } satisfies MemberLevelStats;
  }, 15_000, force);
}

export async function getPointBalance(force = false) {
  return cachedQuery('point-balance', async () => {
    const value = unwrap(await api<{ balance: number } | Envelope<{ balance: number }>>('/points/balance'));
    return Number(value.balance ?? 0);
  }, 10_000, force);
}

export async function getPointHistory(page = 0, size = 10) {
  const value = unwrap(await api<PointHistoryPage | Envelope<PointHistoryPage>>(`/points/history?page=${page}&size=${size}&sort=createdAt,desc`));
  return value.content ?? [];
}
