import { getMemberLevel, getPointHistory } from './member-stats';

export type DishReportSnapshot = {
  level: number;
  grade: string;
  purchaseCount: number;
  savedAmount: number;
  earnedPoints: number;
  remainToNextLevel: number;
};

export async function getDishReportSnapshot(): Promise<DishReportSnapshot> {
  const [level, history] = await Promise.all([getMemberLevel(true), getPointHistory(0, 5)]);
  const latestEarn = history.find(item => item.type === 'EARN');
  return {
    level: level.level,
    grade: level.grade,
    purchaseCount: level.purchaseCount,
    savedAmount: level.savedAmount,
    earnedPoints: Number(latestEarn?.amount ?? 0),
    remainToNextLevel: level.remainToNextLevel,
  };
}
