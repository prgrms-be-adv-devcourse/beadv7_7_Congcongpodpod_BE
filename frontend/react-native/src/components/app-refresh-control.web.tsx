import type { ReactNode } from 'react';

export function AppRefreshControl({ children }: { refreshing: boolean; onRefresh: () => void; children?: ReactNode }) {
  // React Native Web places ScrollView content inside refreshControl. Passing
  // it through preserves the list while native platforms keep pull-to-refresh.
  return children;
}
