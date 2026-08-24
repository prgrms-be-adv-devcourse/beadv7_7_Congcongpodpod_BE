import { BottomTabBar, type BottomTabBarProps } from '@react-navigation/bottom-tabs';
import { StyleSheet, useWindowDimensions, View } from 'react-native';

import { layout, radius, shadow } from '@/constants/theme';

export const FLOATING_TAB_CONTENT_INSET = 136;

export function FloatingTabBar(props: BottomTabBarProps) {
  const { width } = useWindowDimensions();
  const gap = Math.max(20, Math.min(28, props.insets.bottom - 6));
  const barWidth = Math.min(width - gap * 2, layout.content - gap * 2);
  return <View pointerEvents="box-none" style={[styles.stage, { bottom: gap }]}>
    <View style={[styles.bar, { width: barWidth }]}>
      <View style={styles.clip}><BottomTabBar {...props}/></View>
    </View>
  </View>;
}

const styles = StyleSheet.create({
  stage: { position: 'absolute', left: 0, right: 0, height: 64, alignItems: 'center' },
  bar: { height: 64, borderRadius: radius.navigation, ...shadow.nav },
  clip: { flex: 1, overflow: 'hidden', borderRadius: radius.navigation },
});
