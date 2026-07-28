import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/routing/route_paths.dart';
import '../../../domain/model/order.dart';
import '../../../domain/model/order_status.dart';
import '../../../ui/app_colors.dart';
import '../../../ui/app_spacing.dart';
import 'order_detail_view_model.dart';

/// 주문 상세 화면 (B9, `/orders/:orderId`). 2026-07-27 재구현: 실제 `GET /orders/{orderId}`
/// 연동 + 상태별 액션 분기.
///
/// `REJECTED`면 매장이 정한 사유(`rejectReason`, ADR 012)를 보여주고, `CANCELLED`면
/// 문구 없이 "취소된 주문"만 안내한다 — 둘은 별개 상태다(2026-07-26 PO 확정,
/// state-transitions.md §3).
class OrderDetailScreen extends ConsumerWidget {
  const OrderDetailScreen({required this.orderId, super.key});

  final String orderId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final id = int.parse(orderId);
    final orderAsync = ref.watch(orderDetailViewModelProvider(id));

    return Scaffold(
      appBar: AppBar(title: Text('주문 #$orderId')),
      body: orderAsync.when(
        data: (order) => _OrderDetailBody(order: order),
        error: (error, _) => Center(
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

class _OrderDetailBody extends StatelessWidget {
  const _OrderDetailBody({required this.order});

  final Order order;

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
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
                          order.dishName,
                          style: textTheme.titleMedium?.copyWith(
                            color: AppColors.textStrong,
                          ),
                        ),
                      ),
                      Chip(
                        label: Text(orderStatusLabel(order.status)),
                        visualDensity: VisualDensity.compact,
                        padding: EdgeInsets.zero,
                      ),
                    ],
                  ),
                  const SizedBox(height: AppSpacing.sm),
                  _InfoRow('수량', '${order.quantity}개'),
                  _InfoRow('결제 금액', '${order.totalPrice.toInt()}원'),
                  _InfoRow(
                    '픽업 시간',
                    '${order.pickupStartAt.substring(0, 5)} ~ '
                        '${order.pickupEndAt.substring(0, 5)}',
                  ),
                  _InfoRow('연락처', order.phone),
                  // CANCELLED(구매자 취소)는 사유 자체가 없다 — REJECTED(매장 거절)만
                  // rejectReason이 있다. 둘을 같은 문구로 뭉뚱그리지 않는다.
                  if (order.status == 'REJECTED' && order.rejectReason != null) ...[
                    const SizedBox(height: AppSpacing.sm),
                    Text(
                      '매장 안내: ${order.rejectReason}',
                      style: textTheme.bodySmall?.copyWith(color: AppColors.error),
                    ),
                  ] else if (order.status == 'CANCELLED') ...[
                    const SizedBox(height: AppSpacing.sm),
                    Text(
                      '취소된 주문이에요',
                      style: textTheme.bodySmall?.copyWith(color: AppColors.textHint),
                    ),
                  ],
                ],
              ),
            ),
          ),
          const SizedBox(height: AppSpacing.lg),
          if (order.status == 'RESERVED')
            ElevatedButton(
              onPressed: () =>
                  context.push(RoutePaths.orderCancelOf(order.orderId.toString())),
              child: const Text('주문 취소'),
            ),
          if (order.status == 'PICKUP_READY')
            ElevatedButton(
              onPressed: () =>
                  context.push(RoutePaths.orderPickupOf(order.orderId.toString())),
              child: const Text('픽업 확인'),
            ),
        ],
      ),
    );
  }
}

class _InfoRow extends StatelessWidget {
  const _InfoRow(this.label, this.value);

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 2),
      child: Row(
        children: [
          SizedBox(
            width: 72,
            child: Text(label, style: textTheme.bodySmall?.copyWith(color: AppColors.textHint)),
          ),
          Expanded(child: Text(value, style: textTheme.bodyMedium)),
        ],
      ),
    );
  }
}
