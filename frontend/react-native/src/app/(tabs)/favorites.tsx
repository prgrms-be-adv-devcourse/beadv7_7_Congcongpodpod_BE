import { RoundedIcon as Ionicons } from '@/components/rounded-icon';
import { router, useFocusEffect } from 'expo-router';
import { useCallback, useRef, useState } from 'react';
import { FlatList, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { EmptyState } from '@/components/empty-state';
import { FLOATING_TAB_CONTENT_INSET } from '@/components/floating-tab-bar';
import { AppRefreshControl } from '@/components/app-refresh-control';
import { LoadingState } from '@/components/loading-state';
import { RefreshStatus } from '@/components/refresh-status';
import { StoreCard } from '@/components/store-card';
import { colors, fonts, radius } from '@/constants/theme';
import { usePullToRefresh } from '@/hooks/use-pull-to-refresh';
import { useResponsiveLayout } from '@/hooks/use-responsive-layout';
import { useAuth } from '@/providers/auth-provider';
import { showLoginRequired } from '@/lib/login-required';
import { getFavorites } from '@/lib/favorites';
import type { Store } from '@/types/store';

export default function Favorites() {
  const { member, initializing } = useAuth();
  const [favorites, setFavorites] = useState<Store[]>([]);
  const [loading, setLoading] = useState(true);
  const loadedOnce = useRef(false);
  const load = useCallback(async (force = false) => {
    if (!member) return;
    if (!loadedOnce.current) setLoading(true);
    try {
      setFavorites(await getFavorites(force));
    } finally {
      loadedOnce.current = true;
      setLoading(false);
    }
  }, [member]);
  const { refreshing, onRefresh } = usePullToRefresh(() => load(true));
  const { contentWidth, gutter, isCompact } = useResponsiveLayout();
  useFocusEffect(useCallback(() => {
    if (initializing) return;
    if (!member) {
      showLoginRequired('/favorites', () => router.replace('/'));
      return;
    }
    void load();
  }, [initializing, load, member]));

  if (initializing || !member) {
    return <SafeAreaView style={styles.authLoading}><LoadingState compact label="로그인 상태를 확인하고 있어요"/></SafeAreaView>;
  }

  const fixedHeader = <View style={[styles.fixedHeader, { width: contentWidth, paddingHorizontal: gutter }]}><View style={styles.headingRow}><View style={styles.headingCopy}><Text accessibilityRole="header" style={[styles.title, isCompact && styles.titleCompact]}>찜한 매장</Text><Text style={styles.description}>다시 찾고 싶은 마감 할인 매장을 모았어요.</Text></View><View accessibilityLabel={`찜한 매장 ${favorites.length}곳`} style={styles.countBadge}><Ionicons name="heart" size={14} color={colors.ink900}/><Text style={styles.count}>{favorites.length}</Text></View></View></View>;

  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      {fixedHeader}<RefreshStatus visible={refreshing}/>
      <FlatList
        alwaysBounceVertical
        data={favorites}
        keyExtractor={(item) => String(item.storeId)}
        contentContainerStyle={[styles.list, { width: contentWidth, paddingBottom: FLOATING_TAB_CONTENT_INSET }]}
        renderItem={({ item }) => <View style={{ paddingHorizontal: gutter }}><StoreCard compact favorite store={item} onPress={() => router.push({ pathname: '/stores/[storeId]', params: { storeId: String(item.storeId), origin: '/favorites' } })}/></View>}
        ListEmptyComponent={loading ? <LoadingState label="찜한 매장을 확인하고 있어요"/> : <EmptyState title="찜한 매장이 아직 없어요" description="마음에 드는 매장을 찜하면 마감 할인 소식을 빠르게 볼 수 있어요." actionLabel="주변 매장 보기" onAction={() => router.push('/stores')}/>} 
        refreshControl={<AppRefreshControl refreshing={refreshing} onRefresh={onRefresh}/>}
        showsVerticalScrollIndicator={false}
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  authLoading: { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.white },
  safe: { flex: 1, backgroundColor: colors.white },
  list: { alignSelf: 'center', flexGrow: 1 },
  fixedHeader: { alignSelf: 'center', paddingTop: 20, paddingBottom: 12, backgroundColor: colors.white },
  headingRow: { flexDirection: 'row', alignItems: 'flex-start', justifyContent: 'space-between', gap: 16 },
  headingCopy: { flex: 1 },
  title: { color: colors.ink900, fontFamily: fonts.body, fontSize: 28, lineHeight: 35, fontWeight: '900', letterSpacing: -1.1 },
  titleCompact: { fontSize: 25, lineHeight: 32 },
  description: { maxWidth: 500, marginTop: 6, color: colors.ink700, fontFamily: fonts.body, fontSize: 13, lineHeight: 20 },
  countBadge: { minWidth: 44, height: 34, paddingHorizontal: 10, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 5, borderRadius: radius.pill, backgroundColor: colors.canvas, borderWidth: 1, borderColor: colors.line },
  count: { color: colors.ink900, fontFamily: fonts.body, fontSize: 13, fontWeight: '800', fontVariant: ['tabular-nums'] },
});
