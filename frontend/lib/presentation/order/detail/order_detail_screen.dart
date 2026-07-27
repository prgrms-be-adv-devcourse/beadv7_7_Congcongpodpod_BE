import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../core/presentation/components/placeholder_screen.dart';
import '../../../core/routing/route_paths.dart';

/// 주문 상세 화면 (B9, `/orders/:orderId`). 상태별 액션(취소 버튼, 픽업코드
/// 표시)이 실제 목적이지만, 지금은 상태 분기 없이 두 액션 버튼을 항상 보여준다
/// (실제로는 상태에 따라 하나만 활성화됨 — 다음 단계).
///
/// `CANCELLED` 상태가 되면 여기에 취소 사유 안내 문구를 보여줘야 하는데,
/// 그 문구는 매장/시스템이 정한 사유일 때만 존재한다 — 구매자가
/// 직접 취소(B10)한 경우엔 사유 자체가 없어서 문구도 없다(2026-07-26 PO 확정).
/// 실제 상태 분기는 다음 단계.
class OrderDetailScreen extends StatelessWidget {
  const OrderDetailScreen({required this.orderId, super.key});

  final String orderId;

  @override
  Widget build(BuildContext context) {
    return PlaceholderScreen(
      screenCode: 'B9',
      title: '주문 상세',
      description:
          '주문 #$orderId 상세. 상태별 액션(취소 버튼, 픽업코드 표시)이 들어가는 '
          '화면 — 지금은 상태 분기 없이 다음 화면 이동 버튼만 노출.',
      actions: [
        PlaceholderAction(
          '취소하기 → 취소확인(B10)',
          () => context.push(RoutePaths.orderCancelOf(orderId)),
        ),
        PlaceholderAction(
          '픽업 확인(B13)',
          () => context.push(RoutePaths.orderPickupOf(orderId)),
        ),
      ],
    );
  }
}
