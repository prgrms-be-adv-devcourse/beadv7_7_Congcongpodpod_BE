import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../core/presentation/components/placeholder_screen.dart';
import '../../core/routing/route_paths.dart';

/// 예치금 잔액/내역 화면 (B11, `/deposits`). 잔액 조회 +
/// 충전/사용/환불 내역 리스트가 목적. `GET /deposits/balance`,
/// `GET /deposits/history`는 지금 가능하지만 아직 연동 전.
class DepositScreen extends StatelessWidget {
  const DepositScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return PlaceholderScreen(
      screenCode: 'B11',
      title: '예치금 잔액/내역',
      description: '예치금 잔액 조회 + 충전/사용/환불 내역(페이지네이션) 화면.',
      actions: [
        PlaceholderAction(
          '충전하기 → 예치금충전(B14)',
          () => context.push(RoutePaths.depositCharge),
        ),
      ],
    );
  }
}
