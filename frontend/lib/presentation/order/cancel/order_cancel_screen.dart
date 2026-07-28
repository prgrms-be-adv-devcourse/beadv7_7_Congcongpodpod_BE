import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../core/presentation/components/placeholder_screen.dart';

/// 주문 취소 확인 화면 (B10, `/orders/:orderId/cancel`).
///
/// 2026-07-26 PO 확정: 취소 사유는 매장/시스템만 정하고
/// 구매자는 고르지 않는다 — 그래서 이 화면엔 사유 선택 UI가 없다. 구매자는
/// 취소 여부만 확인한다.
class OrderCancelScreen extends StatelessWidget {
  const OrderCancelScreen({required this.orderId, super.key});

  final String orderId;

  @override
  Widget build(BuildContext context) {
    return PlaceholderScreen(
      screenCode: 'B10',
      title: '주문 취소 확인',
      description:
          '주문 #$orderId 취소 확인. 사유 선택 없음 — 취소 사유는 매장/'
          '시스템 전용이라 구매자는 여기서 고르지 않는다.',
      actions: [PlaceholderAction('취소 확인 → 주문상세로', () => context.pop())],
    );
  }
}
