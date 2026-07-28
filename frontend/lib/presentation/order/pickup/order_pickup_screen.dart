import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../core/presentation/components/placeholder_screen.dart';

/// 픽업 확인 화면 (B13, `/orders/:orderId/pickup`). 픽업코드
/// 표시가 목적이나, 발급/조회 API가 아직 없어 화면만 준비.
class OrderPickupScreen extends StatelessWidget {
  const OrderPickupScreen({required this.orderId, super.key});

  final String orderId;

  @override
  Widget build(BuildContext context) {
    return PlaceholderScreen(
      screenCode: 'B13',
      title: '픽업 확인',
      description:
          '주문 #$orderId 픽업코드 표시 화면. 발급/조회 API 없음 — '
          '화면만 준비.',
      actions: [PlaceholderAction('픽업 완료 → 주문상세로', () => context.pop())],
    );
  }
}
