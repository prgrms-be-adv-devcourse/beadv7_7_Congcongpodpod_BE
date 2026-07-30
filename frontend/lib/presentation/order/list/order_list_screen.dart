import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/presentation/components/filter_chip_pill.dart';
import '../../../core/presentation/components/horizontal_fade_scroll.dart';
import '../../../core/presentation/components/info_row.dart';
import '../../../core/presentation/components/order_status_badge.dart';
import '../../../core/presentation/phone_format.dart';
import '../../../core/routing/route_paths.dart';
import '../../../domain/model/order.dart';
import '../../../domain/model/order_status.dart';
import '../../../ui/app_colors.dart';
import '../../../ui/app_spacing.dart';
import 'order_list_view_model.dart';

/// 내 주문 목록 화면 (B8, `/orders`). 하단 탭 중 하나. 상태별(예약/픽업대기/픽업완료/
/// 노쇼/취소/거절, ADR 014) 탭으로 필터링한다.
///
/// 2026-07-27 재구현: 목업 3건 대신 `GET /orders?status=` 실제 연동
/// (store_list_screen.dart의 카테고리 필터와 같은 구조 — 상태는 별도 Provider로
/// 관리하고, 목록 ViewModel이 그걸 구독해서 자동으로 다시 조회한다).
class OrderListScreen extends ConsumerWidget {
  const OrderListScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final ordersAsync = ref.watch(orderListViewModelProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('내 주문목록')),
      body: Column(
        children: [
          const _StatusFilterBar(),
          Expanded(
            child: ordersAsync.when(
              data: (orders) => _OrderListView(orders: orders),
              error: (error, _) => Center(
                child: Padding(
                  padding: const EdgeInsets.all(AppSpacing.lg),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(error.toString(), textAlign: TextAlign.center),
                      const SizedBox(height: AppSpacing.md),
                      ElevatedButton(
                        onPressed: () =>
                            ref.invalidate(orderListViewModelProvider),
                        child: const Text('다시 시도'),
                      ),
                    ],
                  ),
                ),
              ),
              loading: () => const Center(child: CircularProgressIndicator()),
            ),
          ),
        ],
      ),
    );
  }
}

class _StatusFilterBar extends ConsumerWidget {
  const _StatusFilterBar();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final selected = ref.watch(selectedOrderStatusProvider);

    Widget chip(String? value, String label) {
      final isSelected = selected == value;
      return Padding(
        padding: const EdgeInsets.only(right: AppSpacing.xs),
        child: FilterChipPill(
          label: label,
          selected: isSelected,
          onTap: () => ref
              .read(selectedOrderStatusProvider.notifier)
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
        child: HorizontalFadeScroll(
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
    );
  }
}

class _OrderListView extends ConsumerWidget {
  const _OrderListView({required this.orders});

  final List<Order> orders;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    Future<void> refresh() =>
        ref.read(orderListViewModelProvider.notifier).refresh();

    if (orders.isEmpty) {
      return RefreshIndicator(
        onRefresh: refresh,
        child: ListView(
          children: const [
            SizedBox(height: 120),
            Center(
              child: Text(
                '주문 내역이 없어요',
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
        itemCount: orders.length,
        separatorBuilder: (_, _) => const SizedBox(height: AppSpacing.sm),
        itemBuilder: (context, index) => _OrderCard(order: orders[index]),
      ),
    );
  }
}

// 예전엔 카드를 눌러야 주문상세(B9) 화면으로 넘어가서 연락처/거절사유/취소버튼
// 등을 봤는데, 별도 화면일 필요가 없다는 판단(2026-07-30)으로 그 내용을 전부
// 이 카드 안에 합쳤다 — 목록 API가 이미 상세와 같은 Order 모델을 그대로 주기
// 때문에(order_detail_view_model.dart와 필드가 동일) 추가 조회 없이 가능했다.
class _OrderCard extends StatelessWidget {
  const _OrderCard({required this.order});

  final Order order;

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    // CANCELLED(구매자 취소)는 사유 자체가 없다 — REJECTED(매장 거절)만
    // rejectReason이 있다. 둘을 같은 문구로 뭉뚱그리지 않는다.
    final noteText = order.status == 'REJECTED' && order.rejectReason != null
        ? '매장 안내: ${order.rejectReason}'
        : order.status == 'CANCELLED'
        ? '취소된 주문이에요'
        : null;

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
                    order.dishName,
                    style: textTheme.titleMedium?.copyWith(
                      color: AppColors.textStrong,
                    ),
                  ),
                ),
                const SizedBox(width: AppSpacing.sm),
                OrderStatusBadge(status: order.status),
              ],
            ),
            const SizedBox(height: AppSpacing.sm),
            const Divider(height: 1),
            const SizedBox(height: AppSpacing.sm),
            InfoRow(label: '수량', value: '${order.quantity}개'),
            InfoRow(label: '결제 금액', value: '${order.totalPrice.toInt()}원'),
            InfoRow(
              label: '픽업 시간',
              value:
                  '${order.pickupStartAt.substring(0, 5)} ~ '
                  '${order.pickupEndAt.substring(0, 5)}',
            ),
            InfoRow(label: '연락처', value: formatPhone(order.phone)),
            if (noteText != null) ...[
              const SizedBox(height: AppSpacing.sm),
              Container(
                width: double.infinity,
                padding: const EdgeInsets.symmetric(
                  horizontal: AppSpacing.sm,
                  vertical: AppSpacing.xs,
                ),
                decoration: BoxDecoration(
                  color: order.status == 'REJECTED'
                      ? AppColors.errorLight
                      : AppColors.surface,
                  borderRadius: BorderRadius.circular(6),
                ),
                child: Text(
                  noteText,
                  style: textTheme.bodySmall?.copyWith(
                    color: order.status == 'REJECTED'
                        ? AppColors.error
                        : AppColors.textHint,
                  ),
                ),
              ),
            ],
            if (order.status == 'RESERVED') ...[
              const SizedBox(height: AppSpacing.sm),
              ElevatedButton(
                onPressed: () => context.push(
                  RoutePaths.orderCancelOf(order.orderId.toString()),
                ),
                child: const Text('주문 취소'),
              ),
            ] else if (order.status == 'PICKUP_READY') ...[
              const SizedBox(height: AppSpacing.sm),
              ElevatedButton(
                onPressed: () => context.push(
                  RoutePaths.orderPickupOf(order.orderId.toString()),
                ),
                child: const Text('픽업 확인'),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
