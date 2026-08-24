import { Platform, useWindowDimensions } from 'react-native';

import { layout } from '@/constants/theme';

export function useResponsiveLayout() {
  const { width, height } = useWindowDimensions();
  const isTablet = width >= 768;
  const isDesktopWeb = Platform.OS === 'web' && width >= layout.desktop;
  const isCompact = width < 380;
  const gutter = isTablet ? 28 : isCompact ? 14 : 18;
  const contentWidth = Math.min(width, layout.content);
  const wideContentWidth = Math.min(width, layout.wide);

  return { width, height, isTablet, isDesktopWeb, isCompact, gutter, contentWidth, wideContentWidth };
}
