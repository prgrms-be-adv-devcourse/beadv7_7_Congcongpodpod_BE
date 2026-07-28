import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

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
                        onPressed: () => ref.invalidate(orderListViewModelProvider),
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
        child: ChoiceChip(
          label: Text(label),
          selected: isSelected,
          onSelected: (_) => ref
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
        child: SingleChildScrollView(
          scrollDirection: Axis.horizontal,
          child: Row(
            children: [
              chip(null, '전체'),
              for (final value in orderStatusValues) chip(value, orderStatusLabel(value)),
            ],
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

class _OrderCard extends StatelessWidget {
  const _OrderCard({required this.order});

  final Order order;

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    return Card(
      child: ListTile(
        onTap: () =>
            context.push(RoutePaths.orderDetailOf(order.orderId.toString())),
        title: Text(order.dishName),
        subtitle: Text(
          '${order.quantity}개 · ${order.totalPrice.toInt()}원 · '
          '픽업 ${order.pickupStartAt.substring(0, 5)}~${order.pickupEndAt.substring(0, 5)}',
        ),
        trailing: Chip(
          label: Text(orderStatusLabel(order.status)),
          visualDensity: VisualDensity.compact,
          padding: EdgeInsets.zero,
          labelStyle: textTheme.labelSmall,
        ),
      ),
    );
  }
}
