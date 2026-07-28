import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/network/token_storage_provider.dart';
import '../../../core/routing/route_paths.dart';
import '../../../domain/model/store.dart';
import '../../../ui/app_colors.dart';
import '../../../ui/app_spacing.dart';
import 'store_list_view_model.dart';

/// 매장 목록 화면 (B3, 홈 `/`). 로그인 성공 후 랜딩 화면이자 구매자 핵심 경로의 시작점
/// (흐름: B3 매장목록 → B4 매장상세 → ...).
class StoreListScreen extends ConsumerWidget {
  const StoreListScreen({super.key});

  Future<void> _logout(BuildContext context, WidgetRef ref) async {
    final storage = await ref.read(tokenStorageProvider.future);
    await storage.clear();
    // async 이후엔 context가 살아있는지 확인하고 써야 한다(home_screen.dart와 동일한 이유).
    if (context.mounted) {
      context.go(RoutePaths.login);
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    // ref.watch: storeListViewModelProvider의 상태(로딩/성공/실패)가 바뀔 때마다
    // 이 build()가 자동으로 다시 불려서 화면이 최신 상태로 다시 그려진다.
    // setState를 직접 호출할 필요가 없는 이유가 이것 — Riverpod이 대신 해준다.
    final storesAsync = ref.watch(storeListViewModelProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('LastDish'),
        actions: [
          // 장바구니는 하단 탭이 아니라 별도 아이콘으로 두기로 했다(screens.md §5) —
          // 지금은 하단 탭 자체가 없어서 이 아이콘이 장바구니의 유일한 진입점이다.
          IconButton(
            icon: const Icon(Icons.shopping_cart_outlined),
            onPressed: () => context.push(RoutePaths.cart),
          ),
          IconButton(
            icon: const Icon(Icons.logout),
            onPressed: () => _logout(context, ref),
          ),
        ],
      ),
      // AsyncValue.when: data/error/loading 세 가지를 전부 분기해야만 컴파일이 되므로,
      // "로딩 중인데 화면은 데이터를 그리려고 하는" 상태 불일치 버그를 막아준다.
      body: storesAsync.when(
        data: (stores) => _StoreListView(stores: stores),
        error: (error, stackTrace) => _ErrorView(message: error.toString()),
        loading: () => const Center(child: CircularProgressIndicator()),
      ),
    );
  }
}

class _StoreListView extends ConsumerWidget {
  const _StoreListView({required this.stores});

  final List<Store> stores;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    // RefreshIndicator.onRefresh는 Future<void> Function()을 요구한다 —
    // notifier.refresh()가 이미 Future<void>를 반환하므로 그대로 넘기면 된다.
    Future<void> refresh() =>
        ref.read(storeListViewModelProvider.notifier).refresh();

    if (stores.isEmpty) {
      // RefreshIndicator는 스크롤 가능한 자식이 있어야 당기는 제스처를 인식한다 —
      // 목록이 비어 있어도 ListView로 감싸는 이유.
      return RefreshIndicator(
        onRefresh: refresh,
        child: ListView(
          children: const [
            SizedBox(height: 120),
            Center(
              child: Text(
                '주변에 등록된 매장이 없어요',
                style: TextStyle(color: AppColors.textHint),
              ),
            ),
          ],
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: refresh,
      child: ListView.separated(
        padding: const EdgeInsets.all(AppSpacing.md),
        itemCount: stores.length,
        separatorBuilder: (_, _) => const SizedBox(height: AppSpacing.sm),
        itemBuilder: (context, index) => _StoreCard(store: stores[index]),
      ),
    );
  }
}

class _StoreCard extends StatelessWidget {
  const _StoreCard({required this.store});

  final Store store;

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    return Card(
      // 카드 모양(테두리/모서리)은 app_theme.dart의 cardTheme이 전역으로 이미 정해뒀다 —
      // 여기선 내용만 채우면 된다.
      child: InkWell(
        // context.push: go와 달리 현재 화면 위에 "쌓는다" — 상세에서 뒤로가기하면
        // 목록으로 돌아온다(회원가입 화면 진입 때와 같은 방식, login_screen.dart 참고).
        onTap: () =>
            context.push(RoutePaths.storeDetailOf(store.storeId.toString())),
        child: Padding(
          padding: const EdgeInsets.all(AppSpacing.md),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                store.storeName,
                style: textTheme.titleMedium?.copyWith(
                  color: AppColors.textStrong,
                ),
              ),
              const SizedBox(height: AppSpacing.xs),
              Text(
                store.storeAddress,
                style: textTheme.bodySmall?.copyWith(color: AppColors.textBody),
              ),
              const SizedBox(height: AppSpacing.xs),
              Text(
                '영업시간 ${store.openTime} ~ ${store.closeTime}',
                style: textTheme.bodySmall?.copyWith(color: AppColors.textHint),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _ErrorView extends ConsumerWidget {
  const _ErrorView({required this.message});

  final String message;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(AppSpacing.lg),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(message, textAlign: TextAlign.center),
            const SizedBox(height: AppSpacing.md),
            ElevatedButton(
              // ref.invalidate: 이 Provider를 무효화해서 build()를 다시 실행시킨다 —
              // "다시 시도" 버튼의 정석적인 구현 방법.
              onPressed: () => ref.invalidate(storeListViewModelProvider),
              child: const Text('다시 시도'),
            ),
          ],
        ),
      ),
    );
  }
}
