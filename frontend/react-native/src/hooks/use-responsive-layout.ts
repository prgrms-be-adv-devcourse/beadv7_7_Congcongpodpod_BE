import { useWindowDimensions } from 'react-native';

import { layout } from '@/constants/theme';

export function useResponsiveLayout() {
  const { width, height } = useWindowDimensions();
  const isTablet = width >= 768;
  const isCompact = width < 380;
  const gutter = isTablet ? 28 : isCompact ? 14 : 18;
  const contentWidth = Math.min(width, layout.content);

  return { width, height, isTablet, isCompact, gutter, contentWidth };
}
