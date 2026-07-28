import 'package:flutter/material.dart';

import '../../../core/presentation/components/placeholder_screen.dart';

/// 예치금 충전 화면 (B14, `/deposits/charge`). PG 결제로 예치금 충전.
/// `POST /payments`(ready 단계)까지는 백엔드에 있으나 승인(confirm)/콜백
/// 처리가 없어 실제 연동은 백엔드 대기 — 화면만 준비.
class DepositChargeScreen extends StatelessWidget {
  const DepositChargeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return const PlaceholderScreen(
      screenCode: 'B14',
      title: '예치금 충전',
      description:
          'PG 결제로 예치금을 충전하는 화면. 결제 승인/콜백 처리는 '
          '백엔드 대기 — 화면만 준비.',
      actions: [],
    );
  }
}
