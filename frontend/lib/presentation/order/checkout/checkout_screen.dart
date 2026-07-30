import 'package:flutter/foundation.dart' show kDebugMode;
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/presentation/pickup_time_format.dart';
import '../../../core/routing/route_paths.dart';
import '../../../domain/model/order.dart';
import '../../../ui/app_colors.dart';
import '../../../ui/app_spacing.dart';
import '../../cart/cart_view_model.dart';
import '../../deposit/deposit_providers.dart';
import '../list/order_list_view_model.dart';
import '../../store/list/store_list_view_model.dart';
import '../../dish/dish_providers.dart';
import 'checkout_view_model.dart';

/// 주문 확인(체크아웃) 화면 (B7, `/cart/checkout`). 주문을 생성하면 예치금이 즉시
/// 차감된다(ADR 001, 결제=예치금 차감). 장바구니가 상품 1개 단위로 단순화돼 있어
/// (ADR 004), 이 화면도 그 1개만 다룬다.
///
/// 2026-07-28 백엔드 계약 변경(PR #116/#130) 반영: 주문 생성이 `cartItemId`만 받고
/// storeId/dishId/전화번호/픽업시간을 전부 서버가 채운다(전화번호는 회원정보 내부 조회,
/// 픽업시간은 Dish에 설정된 값) — 그래서 이 화면엔 더 이상 입력 폼이 없다.
class CheckoutScreen extends ConsumerWidget {
  const CheckoutScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final cartAsync = ref.watch(cartViewModelProvider);
    final checkoutState = ref.watch(checkoutViewModelProvider);
    final balanceAsync = ref.watch(depositBalanceProvider);
    final textTheme = Theme.of(context).textTheme;

    ref.listen(checkoutViewModelProvider, (previous, next) {
      final wasLoading = previous?.isLoading ?? false;
      if (!wasLoading || next.isLoading) return;

      if (next.hasError) {
        if (kDebugMode) {
          debugPrint('[checkout] ${next.error}\n${next.stackTrace}');
        }
        ScaffoldMessenger.of(context)
          ..hideCurrentSnackBar()
          ..showSnackBar(SnackBar(content: Text(next.error.toString())));
        return;
      }
      final order = next.valueOrNull;
      if (order == null) return; // 초기 상태(null) — 실제 성공 케이스가 아니다.
      _showSuccessDialog(context, ref, order);
    });

    return Scaffold(
      appBar: AppBar(title: const Text('주문 확인')),
      body: cartAsync.when(
        data: (cart) {
          if (cart.items.isEmpty) {
            return const Center(child: Text('장바구니가 비어있어요'));
          }
          final item = cart.items.first;

          return _CheckoutSummary(
            dishName: item.dishName,
            quantity: item.quantity,
            totalPrice: item.subtotalPrice,
            currentBalance: balanceAsync.valueOrNull?.balance,
            isSubmitting: checkoutState.isLoading,
            onSubmit: () => ref
                .read(checkoutViewModelProvider.notifier)
                .submit(cartItemId: item.cartItemId),
            textTheme: textTheme,
          );
        },
        error: (error, _) => Center(child: Text(error.toString())),
        loading: () => const Center(child: CircularProgressIndicator()),
      ),
    );
  }

  void _showSuccessDialog(BuildContext context, WidgetRef ref, Order order) {
    showDialog<void>(
      context: context,
      barrierDismissible: false,
      builder: (dialogContext) => AlertDialog(
        title: const Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.check_circle, size: 48, color: Colors.green),
            SizedBox(height: AppSpacing.sm),
            Text('주문 완료!'),
          ],
        ),
        content: Text(
          '${order.dishName} ${order.quantity}개\n'
          '픽업 시간: ${formatPickupWindow(order.pickupStartAt, order.pickupEndAt)}\n'
          '결제 금액: ${order.totalPrice.toInt()}원',
          textAlign: TextAlign.center,
        ),
        actionsAlignment: MainAxisAlignment.center,
        actions: [
          SizedBox(
            width: double.infinity,
            child: ElevatedButton(
              onPressed: () {
                Navigator.of(dialogContext).pop();
                // 홈/주문내역 탭은 go_router의 StatefulShellRoute(IndexedStack)가 화면을
                // 계속 살려두기 때문에, 여기서 무효화 안 해두면 탭을 다시 봐도 주문 전
                // 캐시가 그대로 남아있는다(2026-07-27 발견 — 주문 후 주문내역 탭이 빈
                // 목록으로 보이다가 필터를 눌러야만 그제서야 새로 불러와졌던 버그).
                // 주문으로 실제 바뀌는 것들을 전부 무효화한다: 장바구니(서버가 비움),
                // 주문 목록(새 주문 추가됨), 예치금(차감됨), 이 상품의 재고(줄어듦).
                ref.invalidate(cartViewModelProvider);
                ref.invalidate(orderListViewModelProvider);
                ref.invalidate(depositBalanceProvider);
                ref.invalidate(depositHistoryProvider);
                ref.invalidate(dishProvider(order.dishId));
                // 홈 화면 카드는 dishProvider가 아니라 nearby 목록 자체(Store.dishes)에서
                // 재고를 보여주므로, 그 목록도 같이 무효화해야 홈으로 돌아갔을 때 줄어든
                // 재고가 바로 반영된다.
                ref.invalidate(storeListViewModelProvider);
                context.go(RoutePaths.home);
              },
              child: const Text('확인'),
            ),
          ),
        ],
      ),
    );
  }
}

class _CheckoutSummary extends StatelessWidget {
  const _CheckoutSummary({
    required this.dishName,
    required this.quantity,
    required this.totalPrice,
    required this.currentBalance,
    required this.isSubmitting,
    required this.onSubmit,
    required this.textTheme,
  });

  final String dishName;
  final int quantity;
  final num totalPrice;
  final num? currentBalance;
  final bool isSubmitting;
  final VoidCallback onSubmit;
  final TextTheme textTheme;

  @override
  Widget build(BuildContext context) {
    final balance = currentBalance;
    return SingleChildScrollView(
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
                  Text(
                    '$dishName × $quantity',
                    style: textTheme.titleMedium?.copyWith(
                      color: AppColors.textStrong,
                    ),
                  ),
                  const SizedBox(height: AppSpacing.xs),
                  Text(
                    '결제 금액 ${totalPrice.toInt()}원 (예치금에서 즉시 차감)',
                    style: textTheme.bodySmall?.copyWith(
                      color: AppColors.textHint,
                    ),
                  ),
                  if (balance != null) ...[
                    const Divider(height: AppSpacing.lg),
                    _BalanceRow(label: '현재 잔액', amount: balance),
                    const SizedBox(height: AppSpacing.xs),
                    _BalanceRow(
                      label: '결제 후 잔액',
                      amount: balance - totalPrice,
                      emphasize: true,
                    ),
                  ],
                ],
              ),
            ),
          ),
          const SizedBox(height: AppSpacing.lg),
          ElevatedButton(
            onPressed: isSubmitting ? null : onSubmit,
            child: isSubmitting
                ? const SizedBox(
                    width: 20,
                    height: 20,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : const Text('주문하기'),
          ),
        ],
      ),
    );
  }
}

class _BalanceRow extends StatelessWidget {
  const _BalanceRow({
    required this.label,
    required this.amount,
    this.emphasize = false,
  });

  final String label;
  final num amount;
  final bool emphasize;

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(
          label,
          style: textTheme.bodySmall?.copyWith(color: AppColors.textHint),
        ),
        Text(
          '${amount.toInt()}원',
          style: textTheme.bodyMedium?.copyWith(
            color: emphasize ? AppColors.primary : AppColors.textBody,
            fontWeight: emphasize ? FontWeight.w700 : FontWeight.w400,
          ),
        ),
      ],
    );
  }
}
