import 'package:flutter/material.dart';

import '../../../ui/app_colors.dart';

/// "라벨: 값" 한 줄. 여러 정보를 `·`로 이어붙인 한 줄보다 읽기 편해서(2026-07-30
/// 주문 카드에서 처음 적용) 비슷한 정보 나열이 필요한 카드에서 재사용한다.
class InfoRow extends StatelessWidget {
  const InfoRow({required this.label, required this.value, super.key});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 2),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 72,
            child: Text(
              label,
              style: textTheme.bodySmall?.copyWith(color: AppColors.textHint),
            ),
          ),
          Expanded(child: Text(value, style: textTheme.bodyMedium)),
        ],
      ),
    );
  }
}
