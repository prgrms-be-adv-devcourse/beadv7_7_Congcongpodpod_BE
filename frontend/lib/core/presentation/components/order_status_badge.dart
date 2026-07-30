import 'package:flutter/material.dart';

import '../../../domain/model/order_status.dart';
import '../../../ui/app_colors.dart';

/// 주문 상태 배지. 예전엔 `Chip`을 그대로 썼는데, 접수/거절/픽업완료 같은
/// 실제 버튼(ElevatedButton/OutlinedButton)과 나란히 있으면 상태 표시도
/// "누를 수 있는 것"처럼 보인다는 피드백(2026-07-29)을 받아 분리했다 —
/// 테두리·그림자 없이 색만 채운 필(pill)로, 버튼과 시각적으로 구분되게 만든다.
/// 색은 app_colors.dart가 이미 정의해둔 "주문 상태 5종 구분용" 시맨틱 컬러를 쓴다.
class OrderStatusBadge extends StatelessWidget {
  const OrderStatusBadge({required this.status, super.key});

  final String status;

  (Color, Color) _colors() {
    switch (status) {
      case 'RESERVED':
        return (AppColors.waiting, AppColors.waitingLight);
      case 'PICKUP_READY':
        return (AppColors.warning, AppColors.warningLight);
      case 'PICKED_UP':
        return (AppColors.success, AppColors.successLight);
      case 'NO_SHOW':
        return (AppColors.noShow, AppColors.noShowLight);
      case 'REJECTED':
        return (AppColors.error, AppColors.errorLight);
      case 'CANCELLED':
      default:
        return (AppColors.textHint, AppColors.surface);
    }
  }

  @override
  Widget build(BuildContext context) {
    final (foreground, background) = _colors();
    final textTheme = Theme.of(context).textTheme;

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: background,
        borderRadius: BorderRadius.circular(999),
      ),
      child: Text(
        orderStatusLabel(status),
        style: textTheme.labelSmall?.copyWith(
          color: foreground,
          fontWeight: FontWeight.w700,
        ),
      ),
    );
  }
}
