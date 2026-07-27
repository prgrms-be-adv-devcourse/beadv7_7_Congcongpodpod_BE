import 'package:flutter/material.dart';

import '../../../core/presentation/components/placeholder_screen.dart';

/// 매장 등록/수정 화면 (S1, `/seller/store`). 매장 정보, 영업시간, 휴무일,
/// 정산계좌 입력이 목적. `POST/PUT /stores` API는 이미 구현됨.
class SellerStoreScreen extends StatelessWidget {
  const SellerStoreScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return const PlaceholderScreen(
      screenCode: 'S1',
      title: '매장 등록/수정',
      description: '매장 정보, 영업시간, 휴무일, 정산계좌 입력 화면.',
      actions: [],
    );
  }
}
