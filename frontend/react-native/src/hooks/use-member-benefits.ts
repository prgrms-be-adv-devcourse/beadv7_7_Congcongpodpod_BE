import { useFocusEffect } from 'expo-router';
import { useCallback, useEffect, useState } from 'react';

import { subscribeMemberBenefitsChanged } from '@/lib/member-benefit-events';
import { getMemberLevel, getPointBalance, type MemberLevelStats } from '@/lib/member-stats';

export function useMemberBenefits(enabled = true) {
  const [level, setLevel] = useState<MemberLevelStats | null>(null);
  const [points, setPoints] = useState<number | null>(null);
  const [loading, setLoading] = useState(enabled);
  const [failed, setFailed] = useState(false);

  const load = useCallback(async (force = false) => {
    if (!enabled) {
      setLevel(null);
      setPoints(null);
      setLoading(false);
      return;
    }
    setLoading(true);
    try {
      const [nextLevel, nextPoints] = await Promise.all([getMemberLevel(force), getPointBalance(force)]);
      setLevel(nextLevel);
      setPoints(nextPoints);
      setFailed(false);
    } catch {
      setFailed(true);
    } finally {
      setLoading(false);
    }
  }, [enabled]);

  useFocusEffect(useCallback(() => { void load(); }, [load]));
  const refresh = useCallback(() => load(true), [load]);
  useEffect(() => {
    if (!enabled) return;
    return subscribeMemberBenefitsChanged(() => { void refresh(); });
  }, [enabled, refresh]);
  return { level, points, loading, failed, refresh };
}
