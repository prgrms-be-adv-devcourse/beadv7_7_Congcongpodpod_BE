import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/routing/route_paths.dart';
import '../../domain/model/cart.dart';
import '../../ui/app_colors.dart';
import '../../ui/app_spacing.dart';
import '../dish/dish_providers.dart';
import 'cart_view_model.dart';

/// 장바구니 화면 (B6, `/cart`). 장바구니는 상품 1개 단위로 단순화돼 있어,
/// 여러 상품을 다루는 리스트 UI 대신 "비었음" / "상품 1개" 두 상태만 그린다.
///
/// "장바구니에 담기" 버튼은 이 화면에 없다 — 그건 상품상세(B5) 화면의 몫인데,
/// B5는 아직 진입 경로가 없어 보류 중이다.
/// 여기선 이미 담긴 상품을 보고/수정/삭제하는 것만 다룬다.
class CartScreen extends ConsumerWidget {
  const CartScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final cartAsync = ref.watch(cartViewModelProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('장바구니')),
      body: cartAsync.when(
        data: (cart) => _CartBody(cart: cart),
        error: (error, stackTrace) => Center(
          child: Padding(
            padding: const EdgeInsets.all(AppSpacing.lg),
            child: Text(error.toString(), textAlign: TextAlign.center),
          ),
        ),
        loading: () => const Center(child: CircularProgressIndicator()),
      ),
    );
  }
}

class _CartBody extends ConsumerStatefulWidget {
  const _CartBody({required this.cart});

  final Cart cart;

  @override
  ConsumerState<_CartBody> createState() => _CartBodyState();
}

class _CartBodyState extends ConsumerState<_CartBody> {
  // 지금 어떤 액션(수량변경/삭제/비우기)이 진행 중인지 — 버튼 중복 탭 방지용.
  //
  // 이걸 cartViewModelProvider의 state.isLoading으로 판단하지 않는 이유: AsyncNotifier의
  // state를 직접 AsyncLoading으로 바꾸면 Riverpod이 "완전히 새로 로딩"으로 취급해서
  // 화면 전체가 스피너로 바뀌어버린다(cart_view_model.dart의 _mutate 주석 참고). 그래서
  // "버튼을 잠깐 막는다"는 순전히 이 화면만의 관심사는 로컬 상태로 따로 관리한다.
  bool _isMutating = false;

  /// 재고 조회가 이미 끝나 있고, 담긴 수량이 그 재고 이상이면 true.
  /// 아직 로딩 중이거나 조회에 실패했으면 모른다는 뜻이라 false(=버튼 그대로 활성).
  bool _isAtStockLimit(CartItem item) {
    final dish = ref.watch(dishProvider(item.dishId)).valueOrNull;
    if (dish == null) return false;
    return item.quantity >= dish.stockQuantity;
  }

  Future<void> _run(Future<void> Function() action) async {
    setState(() => _isMutating = true);
    try {
      await action();
    } catch (error) {
      // cart_view_model.dart의 _mutate 주석 참고 — 실패해도 카트 상태는 안 바뀌니,
      // 여기서 스낵바 하나로 사유(재고 부족 등)만 알려주면 된다.
      if (!mounted) return;
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(SnackBar(content: Text(error.toString())));
    } finally {
      if (mounted) setState(() => _isMutating = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final cart = widget.cart;
    if (cart.items.isEmpty) {
      return const Center(
        child: Text(
          '장바구니가 비어있어요',
          style: TextStyle(color: AppColors.textHint),
        ),
      );
    }

    // items는 항상 0건 또는 1건이라 리스트 위젯 없이 첫 번째만 그린다.
    final item = cart.items.first;
    final textTheme = Theme.of(context).textTheme;
    final notifier = ref.read(cartViewModelProvider.notifier);
    final isBusy = _isMutating;

    return Padding(
      padding: const EdgeInsets.all(AppSpacing.md),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Card(
            child: Padding(
              padding: const EdgeInsets.all(AppSpacing.md),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: Text(
                          item.dishName,
                          style: textTheme.titleMedium?.copyWith(
                            color: AppColors.textStrong,
                          ),
                        ),
                      ),
                      IconButton(
                        icon: const Icon(Icons.delete_outline),
                        color: AppColors.error,
                        onPressed: isBusy
                            ? null
                            : () => _run(
                                () => notifier.removeItem(item.cartItemId),
                              ),
                      ),
                    ],
                  ),
                  const SizedBox(height: AppSpacing.xs),
                  Text(
                    // 백엔드가 BigDecimal(예: 10000.00)로 내려줘서 num 타입인데, 원 단위는
                    // 소수점이 없으니 표시할 땐 정수로 잘라서 보여준다.
                    '개당 ${item.unitPrice.toInt()}원',
                    style: textTheme.bodySmall?.copyWith(
                      color: AppColors.textHint,
                    ),
                  ),
                  const SizedBox(height: 2),
                  // CartItemResponse엔 재고 정보가 없어서(dishId만 있음), 상품 단건
                  // 조회(GET /dishes/{dishId})로 따로 가져온다 — dish_providers.dart 참고.
                  // 로딩/실패 중엔 그냥 재고 표시를 생략한다(수량 버튼 자체는 그대로 동작).
                  ref
                      .watch(dishProvider(item.dishId))
                      .when(
                        data: (dish) => Text(
                          '재고 ${dish.stockQuantity}개',
                          style: textTheme.bodySmall?.copyWith(
                            color: AppColors.textHint,
                          ),
                        ),
                        error: (_, _) => const SizedBox.shrink(),
                        loading: () => const SizedBox.shrink(),
                      ),
                  const SizedBox(height: AppSpacing.sm),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      _QuantityStepper(
                        quantity: item.quantity,
                        // 백엔드 제약: 수량은 1 이상.
                        onDecrement: item.quantity > 1 && !isBusy
                            ? () => _run(
                                () => notifier.updateQuantity(
                                  item.cartItemId,
                                  item.quantity - 1,
                                ),
                              )
                            : null,
                        // 재고 조회에 성공했고 이미 재고만큼 담았으면 +를 눌러도 어차피
                        // 서버가 거절할 걸 미리 막는다 — 재고를 아직 모르면(로딩/실패)
                        // 일단 누르게 두고 서버 응답(재고 부족 스낵바)에 맡긴다.
                        onIncrement: isBusy || _isAtStockLimit(item)
                            ? null
                            : () => _run(
                                () => notifier.updateQuantity(
                                  item.cartItemId,
                                  item.quantity + 1,
                                ),
                              ),
                      ),
                      Text(
                        '${item.subtotalPrice.toInt()}원',
                        style: textTheme.titleMedium?.copyWith(
                          color: AppColors.textStrong,
                          fontWeight: FontWeight.w800,
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ),
          const Spacer(),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text('합계', style: textTheme.titleMedium),
              Text(
                '${cart.totalPrice.toInt()}원',
                style: textTheme.titleLarge?.copyWith(
                  color: AppColors.primary,
                  fontWeight: FontWeight.w800,
                ),
              ),
            ],
          ),
          const SizedBox(height: AppSpacing.md),
          ElevatedButton(
            // 체크아웃(B7)은 아직 API 연동 없는 뼈대 — 전체 플로우 확인용 진입점.
            onPressed: isBusy
                ? null
                : () => context.push(RoutePaths.checkout),
            child: const Text('주문하기'),
          ),
          const SizedBox(height: AppSpacing.sm),
          OutlinedButton(
            onPressed: isBusy ? null : () => _run(notifier.clear),
            child: const Text('전체 비우기'),
          ),
        ],
      ),
    );
  }
}

/// "- 수량 +" 형태의 수량 조절 위젯. 콜백을 null로 주면(예: 최소 수량일 때)
/// 버튼이 자동으로 비활성화된다 — 상위에서 조건을 계산해서 넘겨주는 방식.
class _QuantityStepper extends StatelessWidget {
  const _QuantityStepper({
    required this.quantity,
    required this.onDecrement,
    required this.onIncrement,
  });

  final int quantity;
  final VoidCallback? onDecrement;
  final VoidCallback? onIncrement;

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        IconButton(
          icon: const Icon(Icons.remove_circle_outline),
          onPressed: onDecrement,
        ),
        Text('$quantity', style: Theme.of(context).textTheme.titleMedium),
        IconButton(
          icon: const Icon(Icons.add_circle_outline),
          onPressed: onIncrement,
        ),
      ],
    );
  }
}
