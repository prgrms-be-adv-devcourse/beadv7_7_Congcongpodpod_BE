import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../ui/app_spacing.dart';
import '../../deposit/deposit_providers.dart';
import '../../dish/dish_providers.dart';
import '../../store/list/store_list_view_model.dart';
import '../detail/order_detail_view_model.dart';
import '../list/order_list_view_model.dart';
import '../order_repository_provider.dart';

/// 주문 취소 확인 화면 (B10, `/orders/:orderId/cancel`).
///
/// 2026-07-26 PO 확정: 취소 사유는 매장/시스템만 정하고 구매자는 고르지 않는다 —
/// 그래서 이 화면엔 사유 선택 UI가 없다. 실제로 백엔드 `PATCH /orders/{orderId}/cancel`도
/// 요청 바디 자체를 안 받는 걸로 구현돼 있음(2026-07-27 코드 확인).
class OrderCancelScreen extends ConsumerStatefulWidget {
  const OrderCancelScreen({required this.orderId, super.key});

  final String orderId;

  @override
  ConsumerState<OrderCancelScreen> createState() => _OrderCancelScreenState();
}

class _OrderCancelScreenState extends ConsumerState<OrderCancelScreen> {
  bool _isSubmitting = false;

  Future<void> _confirmCancel() async {
    setState(() => _isSubmitting = true);
    final id = int.parse(widget.orderId);
    // 무효화할 dishId를 취소 전에 미리 챙겨둔다 — 취소 성공 뒤 orderDetail을 무효화하면
    // 이 값도 같이 날아가 버린다.
    final dishId = ref.read(orderDetailViewModelProvider(id)).valueOrNull?.dishId;
    try {
      await ref.read(orderRepositoryProvider).cancelOrder(id);
      // 취소하면 상세/목록의 상태(CANCELLED로 바뀜)뿐 아니라 예치금(환불)과 재고
      // (복원)도 실제로 바뀐다 — checkout_screen.dart의 성공 처리와 같은 이유로
      // 전부 무효화해야 홈/주문내역/예치금 탭이 낡은 캐시를 계속 보여주지 않는다
      // (2026-07-27, 주문 후 주문내역이 안 바뀌던 버그와 같은 원인).
      ref.invalidate(orderDetailViewModelProvider(id));
      ref.invalidate(orderListViewModelProvider);
      ref.invalidate(depositBalanceProvider);
      ref.invalidate(depositHistoryProvider);
      ref.invalidate(storeListViewModelProvider);
      if (dishId != null) ref.invalidate(dishProvider(dishId));
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('주문을 취소했어요')),
      );
      Navigator.of(context).pop();
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(SnackBar(content: Text(error.toString())));
    } finally {
      if (mounted) setState(() => _isSubmitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('주문 취소')),
      body: Padding(
        padding: const EdgeInsets.all(AppSpacing.lg),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text('주문 #${widget.orderId}을(를) 취소할까요?', textAlign: TextAlign.center),
            const SizedBox(height: AppSpacing.sm),
            const Text(
              '취소하면 되돌릴 수 없어요. 예치금은 즉시 복원돼요.',
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: AppSpacing.lg),
            ElevatedButton(
              onPressed: _isSubmitting ? null : _confirmCancel,
              child: _isSubmitting
                  ? const SizedBox(
                      width: 20,
                      height: 20,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : const Text('취소 확인'),
            ),
            const SizedBox(height: AppSpacing.sm),
            OutlinedButton(
              onPressed: _isSubmitting ? null : () => Navigator.of(context).pop(),
              child: const Text('그만두기'),
            ),
          ],
        ),
      ),
    );
  }
}
