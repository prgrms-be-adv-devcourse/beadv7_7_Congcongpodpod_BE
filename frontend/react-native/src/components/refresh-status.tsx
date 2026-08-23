import { Modal, StyleSheet, View } from 'react-native';

import { LoadingState } from '@/components/loading-state';

export function RefreshStatus({ visible }: { visible: boolean }) {
  if (!visible) return null;

  return <Modal animationType="fade" presentationStyle="overFullScreen" transparent visible>
    <View accessibilityLabel="새로고침 중" accessibilityRole="progressbar" style={styles.root}>
      <LoadingState inline label="새로고침 중…" />
    </View>
  </Modal>;
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: 'rgba(255,255,255,0.84)',
  },
});
