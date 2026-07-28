import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../core/routing/route_paths.dart';
import '../../../ui/app_spacing.dart';

/// Toss 결제창이 실패/취소 시 리다이렉트하는 화면 (`/deposits/charge/fail`).
/// Toss가 `code`/`message` 쿼리파라미터로 실패 사유를 실어준다 — 성공 화면과 같은
/// 이유로 `Uri.base.queryParameters`에서 직접 읽는다(go_router 쿼리 아님).
class DepositChargeFailScreen extends StatelessWidget {
  const DepositChargeFailScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final query = Uri.base.queryParameters;
    final message = query['message'] ?? '결제가 취소되었거나 실패했어요.';

    return Scaffold(
      appBar: AppBar(title: const Text('결제 실패')),
      body: Padding(
        padding: const EdgeInsets.all(AppSpacing.md),
        child: Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(Icons.error_outline, size: 48, color: Colors.red),
              const SizedBox(height: AppSpacing.md),
              Text(message, textAlign: TextAlign.center),
              const SizedBox(height: AppSpacing.lg),
              ElevatedButton(
                onPressed: () => context.go(RoutePaths.depositCharge),
                child: const Text('다시 시도'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
