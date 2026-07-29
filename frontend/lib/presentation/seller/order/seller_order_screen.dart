import 'package:flutter/foundation.dart' show kDebugMode;
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/presentation/components/order_status_badge.dart';
import '../../../core/routing/route_paths.dart';
import '../../../domain/model/order.dart';
import '../../../domain/model/order_reject_reason.dart';
import '../../../domain/model/order_status.dart';
import '../../../ui/app_colors.dart';
import '../../../ui/app_spacing.dart';
import '../seller_store_id_provider.dart';
import 'seller_order_view_model.dart';

/// 주문 접수/픽업 처리 화면 (S3, `/seller/orders`).
///
/// 상태 전이는 accept(RESERVED→PICKUP_READY, 픽업코드 발급)/reject(RESERVED→REJECTED,
/// 사유 선택)/pickup(PICKUP_READY→PICKED_UP 또는 NO_SHOW) 3개 API로 나뉜다(2026-07-27
/// `OrderController`/`OrderService` 소스 확인) — `RESERVED`와 `PICKUP_READY`가 서로
/// 다른 액션 버튼을 갖는 이유가 이것 때문이다.
class SellerOrderScreen extends ConsumerWidget {
  const SellerOrderScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final storeIdAsync = ref.watch(sellerStoreIdProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('S3 · 주문 접수/픽업 처리')),
      body: storeIdAsync.when(
        data: (storeId) => storeId == null
            ? Center(
                child: Padding(
                  padding: const EdgeInsets.all(AppSpacing.lg),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      const Text(
                        '먼저 매장을 등록해야 주문을 받을 수 있어요',
                        textAlign: TextAlign.center,
                      ),
                      const SizedBox(height: AppSpacing.md),
                      ElevatedButton(
                        onPressed: () => context.push(RoutePaths.sellerStore),
                        child: const Text('매장 등록하러 가기'),
                      ),
                    ],
                  ),
                ),
              )
            : _OrderQueue(storeId: storeId),
        error: (error, _) => Center(child: Text(error.toString())),
        loading: () => const Center(child: CircularProgressIndicator()),
      ),
    );
  }
}

class _OrderQueue extends ConsumerWidget {
  const _OrderQueue({required this.storeId});

  final int storeId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final ordersAsync = ref.watch(sellerOrderListProvider(storeId));
    final selected = ref.watch(selectedSellerOrderStatusProvider);

    ref.listen(sellerOrderActionViewModelProvider, (previous, next) {
      final wasLoading = previous?.isLoading ?? false;
      if (!wasLoading || next.isLoading) return;

      if (next.hasError) {
        if (kDebugMode)
          debugPrint('[seller_order] ${next.error}\n${next.stackTrace}');
        ScaffoldMessenger.of(context)
          ..hideCurrentSnackBar()
          ..showSnackBar(SnackBar(content: Text(next.error.toString())));
        return;
      }
      if (next.valueOrNull == null) return;
      ref.invalidate(sellerOrderListProvider(storeId));
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('처리했어요')));
    });

    Widget chip(String? value, String label) {
      final isSelected = selected == value;
      return Padding(
        padding: const EdgeInsets.only(right: AppSpacing.xs),
        child: ChoiceChip(
          label: Text(label),
          selected: isSelected,
          onSelected: (_) => ref
              .read(selectedSellerOrderStatusProvider.notifier)
              .select(isSelected ? null : value),
        ),
      );
    }

    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(
            horizontal: AppSpacing.md,
            vertical: AppSpacing.sm,
          ),
          child: SizedBox(
            height: 40,
            child: SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              child: Row(
                children: [
                  chip(null, '전체'),
                  for (final value in orderStatusValues)
                    chip(value, orderStatusLabel(value)),
                ],
              ),
            ),
          ),
        ),
        Expanded(
          child: ordersAsync.when(
            data: (orders) => orders.isEmpty
                ? const Center(child: Text('해당하는 주문이 없어요'))
                : ListView.separated(
                    padding: const EdgeInsets.all(AppSpacing.md),
                    itemCount: orders.length,
                    separatorBuilder: (_, _) =>
                        const SizedBox(height: AppSpacing.sm),
                    itemBuilder: (context, index) =>
                        _SellerOrderCard(order: orders[index]),
                  ),
            error: (error, _) => Center(child: Text('$error')),
            loading: () => const Center(child: CircularProgressIndicator()),
          ),
        ),
      ],
    );
  }
}

class _SellerOrderCard extends ConsumerWidget {
  const _SellerOrderCard({required this.order});

  final Order order;

  Future<void> _pickRejectReason(BuildContext context, WidgetRef ref) async {
    final reason = await showDialog<String>(
      context: context,
      builder: (dialogContext) => SimpleDialog(
        title: const Text('거절 사유 선택'),
        children: [
          for (final value in orderRejectReasonValues)
            SimpleDialogOption(
              onPressed: () => Navigator.of(dialogContext).pop(value),
              child: Text(orderRejectReasonLabel(value)),
            ),
        ],
      ),
    );
    if (reason == null) return;
    ref
        .read(sellerOrderActionViewModelProvider.notifier)
        .reject(orderId: order.orderId, reason: reason);
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final textTheme = Theme.of(context).textTheme;
    final actionState = ref.watch(sellerOrderActionViewModelProvider);
    final notifier = ref.read(sellerOrderActionViewModelProvider.notifier);

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
                    '주문 #${order.orderId} · ${order.dishName}',
                    style: textTheme.titleMedium,
                  ),
                ),
                OrderStatusBadge(status: order.status),
              ],
            ),
            const SizedBox(height: AppSpacing.xs),
            Text(
              '${order.quantity}개 · ${order.totalPrice.toInt()}원 · '
              '픽업 ${order.pickupStartAt.substring(0, 5)}~${order.pickupEndAt.substring(0, 5)} · '
              '연락처 ${order.phone}',
              style: textTheme.bodySmall?.copyWith(color: AppColors.textHint),
            ),
            const SizedBox(height: AppSpacing.sm),
            Wrap(
              spacing: AppSpacing.sm,
              children: [
                if (order.status == 'RESERVED') ...[
                  ElevatedButton(
                    onPressed: actionState.isLoading
                        ? null
                        : () => notifier.accept(order.orderId),
                    child: const Text('접수'),
                  ),
                  OutlinedButton(
                    onPressed: actionState.isLoading
                        ? null
                        : () => _pickRejectReason(context, ref),
                    child: const Text('거절'),
                  ),
                ] else if (order.status == 'PICKUP_READY') ...[
                  ElevatedButton(
                    onPressed: actionState.isLoading
                        ? null
                        : () => notifier.updatePickupStatus(
                            orderId: order.orderId,
                            status: 'PICKED_UP',
                          ),
                    child: const Text('픽업완료'),
                  ),
                  OutlinedButton(
                    onPressed: actionState.isLoading
                        ? null
                        : () => notifier.updatePickupStatus(
                            orderId: order.orderId,
                            status: 'NO_SHOW',
                          ),
                    child: const Text('노쇼 처리'),
                  ),
                ],
              ],
            ),
          ],
        ),
      ),
    );
  }
}
