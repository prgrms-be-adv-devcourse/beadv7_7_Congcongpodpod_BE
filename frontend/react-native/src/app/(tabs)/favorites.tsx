import { Ionicons } from '@expo/vector-icons';
import { router, useFocusEffect } from 'expo-router';
import { useCallback } from 'react';
import { FlatList, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { EmptyState } from '@/components/empty-state';
import { AppRefreshControl } from '@/components/app-refresh-control';
import { LoadingState } from '@/components/loading-state';
import { RefreshStatus } from '@/components/refresh-status';
import { StoreCard } from '@/components/store-card';
import { colors, fonts, radius } from '@/constants/theme';
import { useNearbyStores } from '@/hooks/use-nearby-stores';
import { usePullToRefresh } from '@/hooks/use-pull-to-refresh';
import { useResponsiveLayout } from '@/hooks/use-responsive-layout';
import { useAuth } from '@/providers/auth-provider';
import { showLoginRequired } from '@/lib/login-required';

export default function Favorites() {
  const { member, initializing } = useAuth();
  const { stores, loading, reload } = useNearbyStores(5);
  const { refreshing, onRefresh } = usePullToRefresh(reload);
  const { contentWidth, gutter, isCompact } = useResponsiveLayout();
  const favorites = stores.slice(0, 3);

  useFocusEffect(useCallback(() => {
    if (!initializing && !member) showLoginRequired('/favorites', () => router.replace('/'));
  }, [initializing, member]));

  if (initializing || !member) {
    return <SafeAreaView style={styles.authLoading}><LoadingState compact label="로그인 상태를 확인하고 있어요"/></SafeAreaView>;
  }

  const fixedHeader = <View style={[styles.fixedHeader, { width: contentWidth, paddingHorizontal: gutter }]}><View style={styles.headingRow}><View style={styles.headingCopy}><Text style={[styles.title, isCompact && styles.titleCompact]}>찜한 매장</Text><Text style={styles.description}>다시 찾고 싶은 마감 할인 매장을 모았어요.</Text></View><View style={styles.countBadge}><Ionicons name="heart" size={14} color={colors.ink900}/><Text style={styles.count}>{favorites.length}</Text></View></View></View>;
  const header = (
    <View style={[styles.header, { paddingHorizontal: gutter }]}> 
      <View style={styles.notice}>
        <View style={styles.noticeIcon}><Ionicons name="notifications-outline" size={18} color={colors.green700}/></View>
        <View style={styles.noticeCopy}><Text style={styles.noticeTitle}>오늘 마감 소식을 확인하세요</Text><Text style={styles.noticeText}>판매 중인 상품과 남은 수량은 매장 상세에서 바로 확인할 수 있어요.</Text></View>
      </View>
      <View style={styles.listHead}><Text style={styles.listTitle}>저장한 매장</Text><Text style={styles.listMeta}>최근 추가순</Text></View>
    </View>
  );

  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      {fixedHeader}<RefreshStatus visible={refreshing}/>
      <FlatList
        alwaysBounceVertical
        data={favorites}
        keyExtractor={(item) => String(item.storeId)}
        ListHeaderComponent={header}
        contentContainerStyle={[styles.list, { width: contentWidth, paddingBottom: 24 }]}
        ItemSeparatorComponent={() => <View style={styles.separator}/>} 
        renderItem={({ item }) => <View style={{ paddingHorizontal: gutter }}><StoreCard favorite store={item} onPress={() => router.push({ pathname: '/stores/[storeId]', params: { storeId: String(item.storeId), origin: '/favorites' } })}/></View>}
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
  fixedHeader: { alignSelf: 'center', paddingTop: 20, paddingBottom: 12, backgroundColor: colors.white }, header: { paddingBottom: 12 },
  headingRow: { flexDirection: 'row', alignItems: 'flex-start', justifyContent: 'space-between', gap: 16 },
  headingCopy: { flex: 1 },
  title: { color: colors.ink900, fontFamily: fonts.body, fontSize: 28, lineHeight: 35, fontWeight: '900', letterSpacing: -1.1 },
  titleCompact: { fontSize: 25, lineHeight: 32 },
  description: { maxWidth: 500, marginTop: 6, color: colors.ink700, fontFamily: fonts.body, fontSize: 13, lineHeight: 20 },
  countBadge: { minWidth: 44, height: 34, paddingHorizontal: 10, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 5, borderRadius: radius.pill, backgroundColor: colors.canvas, borderWidth: 1, borderColor: colors.line },
  count: { color: colors.ink900, fontFamily: fonts.body, fontSize: 13, fontWeight: '900' },
  notice: { marginTop: 19, padding: 13, flexDirection: 'row', alignItems: 'center', gap: 11, borderRadius: 11, backgroundColor: colors.green50, borderWidth: 1, borderColor: colors.green100 },
  noticeIcon: { width: 36, height: 36, alignItems: 'center', justifyContent: 'center', borderRadius: 18, backgroundColor: colors.white },
  noticeCopy: { flex: 1 },
  noticeTitle: { color: colors.ink900, fontFamily: fonts.body, fontSize: 13, fontWeight: '800' },
  noticeText: { marginTop: 3, color: colors.ink700, fontFamily: fonts.body, fontSize: 11, lineHeight: 16 },
  listHead: { marginTop: 23, paddingBottom: 10, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  listTitle: { color: colors.ink900, fontFamily: fonts.body, fontSize: 18, fontWeight: '900', letterSpacing: -0.45 },
  listMeta: { color: colors.ink500, fontFamily: fonts.body, fontSize: 11, fontWeight: '600' },
  separator: { height: 10 },
});
