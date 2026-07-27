import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../core/presentation/components/placeholder_screen.dart';
import '../../../core/routing/route_paths.dart';

/// 주문 확인(체크아웃) 화면 (B7, `/cart/checkout`). 픽업 시간 확인 후
/// 주문을 생성하면 예치금이 즉시 차감된다. 지금은 API 연동 없이
/// 다음 화면(B8)으로 넘어가는 버튼만 있는 뼈대.
class CheckoutScreen extends StatelessWidget {
  const CheckoutScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return PlaceholderScreen(
      screenCode: 'B7',
      title: '주문 확인(체크아웃)',
      description:
          '픽업 시간을 확인하고 주문을 생성하는 화면. 주문 생성 시 예치금이 즉시 '
          '차감된다. 예치금 부족 시 실패 사유가 표시된다.',
      actions: [
        PlaceholderAction(
          '주문하기 → 내 주문목록(B8)',
          () => context.go(RoutePaths.orders),
        ),
      ],
    );
  }
}
