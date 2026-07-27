import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../ui/app_colors.dart';
import '../../../ui/app_spacing.dart';
import 'order_pickup_view_model.dart';

/// 픽업 확인 화면 (B13, `/orders/:orderId/pickup`). 2026-07-27 재구현: ADR 013 기준
/// 실제 픽업코드 조회(`GET /orders/{orderId}/pickupCode`) 연동 — 랜덤 숫자 6자리를
/// 매장 직원에게 보여주는 용도. 닉네임은 안내용일 뿐 본인 확인 수단이 아니라서
/// (ADR 013) 이 화면엔 코드만 크게 보여준다.
class OrderPickupScreen extends ConsumerWidget {
  const OrderPickupScreen({required this.orderId, super.key});

  final String orderId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final id = int.parse(orderId);
    final pickupAsync = ref.watch(orderPickupViewModelProvider(id));
    final textTheme = Theme.of(context).textTheme;

    return Scaffold(
      appBar: AppBar(title: const Text('픽업 확인')),
      body: Center(
        child: pickupAsync.when(
          data: (pickup) => Padding(
            padding: const EdgeInsets.all(AppSpacing.lg),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(pickup.dishName, style: textTheme.titleMedium),
                const SizedBox(height: AppSpacing.lg),
                Text(
                  '이 코드를 매장 직원에게 보여주세요',
                  style: textTheme.bodySmall?.copyWith(color: AppColors.textHint),
                ),
                const SizedBox(height: AppSpacing.sm),
                Text(
                  pickup.pickupCode,
                  style: textTheme.displayMedium?.copyWith(
                    color: AppColors.primary,
                    fontWeight: FontWeight.w800,
                    letterSpacing: 4,
                  ),
                ),
                const SizedBox(height: AppSpacing.lg),
                Text(
                  '픽업 시간 ${pickup.pickupStartAt.substring(0, 5)} ~ '
                  '${pickup.pickupEndAt.substring(0, 5)}',
                  style: textTheme.bodySmall?.copyWith(color: AppColors.textHint),
                ),
              ],
            ),
          ),
          error: (error, _) => Padding(
            padding: const EdgeInsets.all(AppSpacing.lg),
            child: Text(
              '지금은 픽업코드를 볼 수 없어요\n$error',
              textAlign: TextAlign.center,
            ),
          ),
          loading: () => const CircularProgressIndicator(),
        ),
      ),
    );
  }
}
