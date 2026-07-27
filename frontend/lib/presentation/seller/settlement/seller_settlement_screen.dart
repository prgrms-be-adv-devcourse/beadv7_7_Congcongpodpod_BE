import 'package:flutter/material.dart';

import '../../../core/presentation/components/placeholder_screen.dart';

/// 정산 조회 화면 (S4, `/seller/settlements`). 정산 내역 조회가 목적.
/// 컨트롤러 자체가 없음(패키지만 존재) — 백엔드 대기.
class SellerSettlementScreen extends StatelessWidget {
  const SellerSettlementScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return const PlaceholderScreen(
      screenCode: 'S4',
      title: '정산 조회',
      description: '정산 내역 조회 화면. 컨트롤러 없음 — 백엔드 대기.',
      actions: [],
    );
  }
}
