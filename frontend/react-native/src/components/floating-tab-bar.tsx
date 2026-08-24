import { BottomTabBar, type BottomTabBarProps } from '@react-navigation/bottom-tabs';
import { StyleSheet, View } from 'react-native';

export const FLOATING_TAB_CONTENT_INSET = 128;

export function FloatingTabBar(props: BottomTabBarProps) {
  const gap = Math.max(20, Math.min(28, props.insets.bottom - 6));
  return <View pointerEvents="box-none" style={[styles.stage, { left: gap, right: gap, bottom: gap }]}>
    <View style={styles.clip}><BottomTabBar {...props}/></View>
  </View>;
}

const styles = StyleSheet.create({
  stage: { position: 'absolute', height: 64, borderRadius: 26, shadowColor: '#17281C', shadowOpacity: 0.16, shadowRadius: 16, shadowOffset: { width: 0, height: 6 }, elevation: 9 },
  clip: { flex: 1, overflow: 'hidden', borderRadius: 26 },
});
