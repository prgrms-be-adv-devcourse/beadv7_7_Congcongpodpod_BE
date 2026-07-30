import 'package:flutter/material.dart';

import '../../../ui/app_colors.dart';

/// 필터 칩(카테고리/상태 선택용). `ChoiceChip`을 쓰다가 Flutter Web(CanvasKit)에서
/// 짧은 한글 라벨의 마지막 글자가 잘리는 렌더링 문제를 겪어서, 그 내부 레이아웃
/// 로직을 아예 안 타는 직접 구현으로 바꿨다 — Text + Padding만 쓰는 단순한 구조라
/// 폭 계산이 어긋날 여지가 없다.
class FilterChipPill extends StatelessWidget {
  const FilterChipPill({
    required this.label,
    required this.selected,
    required this.onTap,
    super.key,
  });

  final String label;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    return Material(
      color: selected ? AppColors.primaryLight : AppColors.surface,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(999),
        side: BorderSide(
          color: selected ? AppColors.primary : AppColors.border,
        ),
      ),
      child: InkWell(
        borderRadius: BorderRadius.circular(999),
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
          child: Text(
            label,
            softWrap: false,
            style: textTheme.labelLarge?.copyWith(
              color: selected ? AppColors.primaryDark : AppColors.textBody,
              fontWeight: selected ? FontWeight.w700 : FontWeight.w500,
            ),
          ),
        ),
      ),
    );
  }
}
