import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/routing/route_paths.dart';
import '../../../ui/app_spacing.dart';
import 'deposit_charge_success_view_model.dart';

/// Toss 결제창이 성공 시 리다이렉트하는 화면 (`/deposits/charge/success`).
/// 이 앱은 hash 라우팅이라 Toss가 붙인 쿼리(`paymentKey`/`orderId`/`amount`)가
/// go_router의 `state.uri.queryParameters`가 아니라 브라우저 URL의 `#` 앞부분에
/// 실린다 — 그래서 `Uri.base.queryParameters`로 직접 읽는다
/// (deposit_charge_view_model.dart의 successUrl 조립 부분 참고).
class DepositChargeSuccessScreen extends ConsumerStatefulWidget {
  const DepositChargeSuccessScreen({super.key});

  @override
  ConsumerState<DepositChargeSuccessScreen> createState() =>
      _DepositChargeSuccessScreenState();
}

class _DepositChargeSuccessScreenState
    extends ConsumerState<DepositChargeSuccessScreen> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _approve());
  }

  void _approve() {
    final query = Uri.base.queryParameters;
    final paymentKey = query['paymentKey'];
    final orderId = query['orderId'];
    final amount = num.tryParse(query['amount'] ?? '');

    if (paymentKey == null || orderId == null || amount == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('결제 정보를 읽지 못했어요. 다시 시도해 주세요.')),
      );
      return;
    }

    ref
        .read(depositChargeSuccessViewModelProvider.notifier)
        .approve(paymentKey: paymentKey, orderId: orderId, amount: amount);
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(depositChargeSuccessViewModelProvider);
    final textTheme = Theme.of(context).textTheme;

    return Scaffold(
      appBar: AppBar(title: const Text('충전 처리 중')),
      body: Padding(
        padding: const EdgeInsets.all(AppSpacing.md),
        child: Center(
          child: state.when(
            data: (approve) {
              if (approve == null) {
                return const CircularProgressIndicator();
              }
              return Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Icon(Icons.check_circle, size: 48, color: Colors.green),
                  const SizedBox(height: AppSpacing.md),
                  Text('충전 완료!', style: textTheme.titleLarge),
                  const SizedBox(height: AppSpacing.sm),
                  Text('현재 잔액 ${approve.depositBalance.toInt()}원'),
                  const SizedBox(height: AppSpacing.lg),
                  ElevatedButton(
                    onPressed: () => context.go(RoutePaths.deposits),
                    child: const Text('예치금 화면으로'),
                  ),
                ],
              );
            },
            loading: () => const Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                CircularProgressIndicator(),
                SizedBox(height: AppSpacing.md),
                Text('결제 승인 확인 중...'),
              ],
            ),
            error: (error, _) => Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Icon(Icons.error_outline, size: 48, color: Colors.red),
                const SizedBox(height: AppSpacing.md),
                Text('충전 승인에 실패했어요\n$error', textAlign: TextAlign.center),
                const SizedBox(height: AppSpacing.lg),
                ElevatedButton(
                  onPressed: () => context.go(RoutePaths.depositCharge),
                  child: const Text('다시 시도'),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
