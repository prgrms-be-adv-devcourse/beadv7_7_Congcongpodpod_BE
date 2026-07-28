import 'package:flutter/material.dart';

import '../../../ui/app_colors.dart';
import '../../../ui/app_spacing.dart';

/// 워킹 스켈레톤 공용 화면 뼈대. API/ViewModel 없이 "이 화면이 무엇인지"와
/// "다음 화면으로 어떻게 넘어가는지"만 보여준다.
/// (이미 정의된 화면명/목적/관련 요구사항을 그대로 표시만 함 — 새 기획 결정 아님)
class PlaceholderScreen extends StatelessWidget {
  const PlaceholderScreen({
    required this.screenCode,
    required this.title,
    required this.description,
    this.actions = const [],
    super.key,
  });

  final String screenCode;
  final String title;
  final String description;
  final List<PlaceholderAction> actions;

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    return Scaffold(
      appBar: AppBar(title: Text('$screenCode · $title')),
      body: Padding(
        padding: const EdgeInsets.all(AppSpacing.lg),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Container(
              padding: const EdgeInsets.all(AppSpacing.md),
              decoration: BoxDecoration(
                color: AppColors.surface,
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: AppColors.border),
              ),
              child: Text(
                description,
                style: textTheme.bodyMedium?.copyWith(
                  color: AppColors.textBody,
                ),
              ),
            ),
            const SizedBox(height: AppSpacing.xl),
            for (final action in actions) ...[
              ElevatedButton(
                onPressed: action.onTap,
                child: Text(action.label),
              ),
              const SizedBox(height: AppSpacing.sm),
            ],
          ],
        ),
      ),
    );
  }
}

class PlaceholderAction {
  const PlaceholderAction(this.label, this.onTap);

  final String label;
  final VoidCallback onTap;
}
