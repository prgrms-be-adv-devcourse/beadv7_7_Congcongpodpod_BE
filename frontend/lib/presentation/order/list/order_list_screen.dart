import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../core/routing/route_paths.dart';
import '../../../ui/app_colors.dart';
import '../../../ui/app_spacing.dart';

/// 내 주문 목록 화면 (B8, `/orders`). 하단 탭 중 하나. 상태별(예약/픽업대기/
/// 완료/노쇼/취소) 조회가 실제 목적이지만, 지금은 API 없이
/// 목업 주문 3건만 고정으로 보여준다.
class OrderListScreen extends StatelessWidget {
  const OrderListScreen({super.key});

  static const _sampleOrders = [
    (id: '1', label: '주문 #1 · 예약(RESERVED)'),
    (id: '2', label: '주문 #2 · 픽업대기(PICKUP_READY)'),
    (id: '3', label: '주문 #3 · 픽업완료(PICKED_UP)'),
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('B8 · 내 주문목록')),
      body: ListView.separated(
        padding: const EdgeInsets.all(AppSpacing.md),
        itemCount: _sampleOrders.length,
        separatorBuilder: (_, _) => const SizedBox(height: AppSpacing.sm),
        itemBuilder: (context, index) {
          final order = _sampleOrders[index];
          return Card(
            child: ListTile(
              title: Text(order.label),
              subtitle: const Text(
                '목업 데이터 — 상태 탭은 다음 단계',
                style: TextStyle(color: AppColors.textHint),
              ),
              onTap: () => context.push(RoutePaths.orderDetailOf(order.id)),
            ),
          );
        },
      ),
    );
  }
}
