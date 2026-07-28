import 'package:flutter/material.dart';

import '../../../core/presentation/components/placeholder_screen.dart';

/// 상품 등록/관리 화면 (S2, `/seller/dishes`). 상품 CRUD, 상태 변경
/// (판매중/품절/마감)이 목적. `POST/PUT/PATCH /dishes` API는 구현됨.
class SellerDishScreen extends StatelessWidget {
  const SellerDishScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return const PlaceholderScreen(
      screenCode: 'S2',
      title: '상품 등록/관리',
      description: '상품 CRUD, 상태 변경(판매중/품절/마감) 화면.',
      actions: [],
    );
  }
}
