import { RefreshControl } from 'react-native';

export function AppRefreshControl({ refreshing, onRefresh }: { refreshing: boolean; onRefresh: () => void }) {
  return (
    <RefreshControl
      accessibilityLabel="새로운 내용을 불러오는 중"
      colors={['transparent']}
      onRefresh={onRefresh}
      progressBackgroundColor="transparent"
      refreshing={refreshing}
      tintColor="transparent"
    />
  );
}
