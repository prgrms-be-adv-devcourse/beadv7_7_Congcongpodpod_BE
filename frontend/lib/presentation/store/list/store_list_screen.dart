import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/domain/error/app_exception.dart';
import '../../../core/network/token_storage_provider.dart';
import '../../../core/presentation/components/filter_chip_pill.dart';
import '../../../core/routing/route_paths.dart';
import '../../../domain/model/category.dart';
import '../../../domain/model/dish.dart';
import '../../../domain/model/store.dart';
import '../../../ui/app_colors.dart';
import '../../../ui/app_spacing.dart';
import '../../cart/cart_repository_provider.dart';
import 'store_list_view_model.dart';

/// 매장 목록 화면 (B3, 홈 `/`). 로그인 성공 후 랜딩 화면이자 구매자 핵심 경로의 시작점.
///
/// 2026-07-27 재구현: 백엔드가 `category` 필터와 `dishes[]` 임베딩을 반영하면서(ADR
/// 017/018), 매장상세(B4)/상품상세(B5) 화면 없이 이 화면 카드 안에서 카테고리 필터 →
/// 상품 확인 → 장바구니 담기까지 전부 끝내는 구조로 바뀌었다.
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
      body: Column(
        children: [
          const _CategoryFilterBar(),
          Expanded(
            // AsyncValue.when: data/error/loading 세 가지를 전부 분기해야만 컴파일이 되므로,
            // "로딩 중인데 화면은 데이터를 그리려고 하는" 상태 불일치 버그를 막아준다.
            child: storesAsync.when(
              data: (stores) => _StoreListView(stores: stores),
              error: (error, stackTrace) => _ErrorView(message: error.toString()),
              loading: () => const Center(child: CircularProgressIndicator()),
            ),
          ),
        ],
      ),
    );
  }
}

/// 카테고리 필터 칩 한 줄("전체" + 15개). 가로 스크롤 — 화면 폭에 다 안 들어가서.
class _CategoryFilterBar extends ConsumerWidget {
  const _CategoryFilterBar();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final selected = ref.watch(selectedStoreCategoryProvider);

    Widget chip(String? value, String label) {
      final isSelected = selected == value;
      return Padding(
        padding: const EdgeInsets.only(right: AppSpacing.xs),
        child: FilterChipPill(
          label: label,
          selected: isSelected,
          // 이미 선택된 칩을 다시 누르면 "전체"로 돌아간다 — 매번 다른 칩을
          // 눌러 해제해야 하는 것보다 자연스럽다.
          onTap: () => ref
              .read(selectedStoreCategoryProvider.notifier)
              .select(isSelected ? null : value),
        ),
      );
    }

    return Padding(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.md,
        vertical: AppSpacing.sm,
      ),
      child: SizedBox(
        height: 40,
        // ListView 대신 SingleChildScrollView + Row를 쓴다 — 화면에 ListView가
        // 이미 하나 더 있어서(매장 목록), 위젯 타입으로 찾는 테스트/도구가
        // 둘을 혼동하지 않게 하려는 것도 이유 중 하나다.
        child: SingleChildScrollView(
          scrollDirection: Axis.horizontal,
          child: Row(
            children: [
              chip(null, '전체'),
              for (final value in categoryValues) chip(value, categoryLabel(value)),
            ],
          ),
        ),
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

/// 매장 카드. 더 이상 탭해서 상세 화면으로 안 넘어간다(B4 폐기) — 카드 자체가
/// 매장 정보 + 판매중 상품(0~1개) + 담기 버튼까지 전부 보여준다.
class _StoreCard extends StatelessWidget {
  const _StoreCard({required this.store});

  final Store store;

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(AppSpacing.md),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(
                    store.storeName,
                    style: textTheme.titleMedium?.copyWith(
                      color: AppColors.textStrong,
                    ),
                  ),
                ),
                // Material Chip 계열은 Flutter Web에서 한글 라벨 끝글자를 잘라먹는
                // 렌더링 버그가 있다(troubleshooting 010 — ChoiceChip에서 먼저 발견돼
                // FilterChipPill로 교체했던 것과 같은 문제, 여기 카드 배지는 그때
                // 빠뜨렸다가 2026-07-30에 재발견). Chip 자체를 안 쓰고 Container+Text로
                // 직접 그린다.
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  decoration: BoxDecoration(
                    color: AppColors.primaryLight,
                    borderRadius: BorderRadius.circular(999),
                  ),
                  child: Text(
                    categoryLabel(store.category),
                    softWrap: false,
                    style: textTheme.labelSmall?.copyWith(color: AppColors.primaryDark),
                  ),
                ),
              ],
            ),
            const SizedBox(height: AppSpacing.xs),
            Text(
              store.storeAddress,
              style: textTheme.bodySmall?.copyWith(color: AppColors.textBody),
            ),
            const SizedBox(height: AppSpacing.xs),
            Text(
              // 서버는 "09:00:00"(초 포함)으로 내려준다 — order_list_screen.dart의
              // 픽업 시간 표시와 같은 이유로 앞 5자리(HH:mm)만 보여준다.
              '영업시간 ${store.openTime.substring(0, 5)} ~ ${store.closeTime.substring(0, 5)}',
              style: textTheme.bodySmall?.copyWith(color: AppColors.textHint),
            ),
            const Divider(height: AppSpacing.lg),
            // dishes가 빈 리스트 = 지금 판매중인 상품이 없음(품절/마감 포함,
            // 서버가 이미 ON_SALE만 걸러서 준다 — ADR 018).
            if (store.dishes.isEmpty)
              Text(
                '지금 판매중인 상품이 없어요',
                style: textTheme.bodySmall?.copyWith(color: AppColors.textHint),
              )
            else
              for (final dish in store.dishes) _DishRow(dish: dish),
          ],
        ),
      ),
    );
  }
}

/// 상품 1건 표시 + 담기 버튼. ADR 004(매장≈상품 1개)라 지금은 매장당 최대 1줄만 그려진다.
class _DishRow extends ConsumerStatefulWidget {
  const _DishRow({required this.dish});

  final Dish dish;

  @override
  ConsumerState<_DishRow> createState() => _DishRowState();
}

class _DishRowState extends ConsumerState<_DishRow> {
  // 중복 탭 방지용 로컬 상태. cart_screen.dart의 `_isMutating`과 같은 이유 —
  // Riverpod state를 직접 로딩으로 바꾸면 화면 전체가 스피너로 바뀌어버리는 문제를
  // 피하려고, "지금 이 버튼 하나만 처리 중"이라는 걸 위젯 로컬 상태로만 관리한다.
  bool _isAdding = false;

  Future<void> _addToCart() async {
    setState(() => _isAdding = true);
    try {
      final cartRepository = ref.read(cartRepositoryProvider);
      // Cart는 회원가입 시 자동 생성되고, 이 첫 조회에서 cartId를 얻는다
      // (별도 "장바구니 생성" API가 없음 — api-contracts.md Cart 절 참고).
      final cart = await cartRepository.getMyCart();
      await cartRepository.addItem(
        cartId: cart.cartId,
        dishId: widget.dish.dishId,
        quantity: 1,
      );
      if (!mounted) return;
      // 장바구니는 상품 1개 단위(ADR 004)라 "담기"가 곧 "장바구니 전체를
      // 정한다"는 뜻이다 — 담고 나서 홈에 머물 이유가 없어서 바로 장바구니로
      // 넘긴다.
      context.push(RoutePaths.cart);
    } on AppException catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.message)));
    } finally {
      if (mounted) setState(() => _isAdding = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    final dish = widget.dish;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: AppSpacing.xs),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(dish.dishName, style: textTheme.bodyMedium),
                const SizedBox(height: 2),
                Row(
                  children: [
                    Text(
                      '${dish.dishPrice.toInt()}원',
                      style: textTheme.bodySmall?.copyWith(
                        color: AppColors.textHint,
                        decoration: TextDecoration.lineThrough,
                      ),
                    ),
                    const SizedBox(width: AppSpacing.xs),
                    Text(
                      '${dish.discountPrice.toInt()}원',
                      style: textTheme.titleSmall?.copyWith(
                        color: AppColors.primary,
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 2),
                Text(
                  '재고 ${dish.stockQuantity}개',
                  style: textTheme.bodySmall?.copyWith(color: AppColors.textHint),
                ),
              ],
            ),
          ),
          FilledButton(
            onPressed: _isAdding ? null : _addToCart,
            child: _isAdding
                ? const SizedBox(
                    width: 16,
                    height: 16,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : const Text('담기'),
          ),
        ],
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
