import 'package:flutter/foundation.dart' show kDebugMode;
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../ui/app_spacing.dart';
import 'deposit_charge_view_model.dart';

/// 예치금 충전 화면 (B14, `/deposits/charge`). 금액 입력 → Toss 결제창(카드)으로
/// 이동 → 승인되면 예치금이 서버에서 즉시 충전된다. Toss가 결제 성공 시 페이지
/// 자체를 리다이렉트하므로, 이 화면에서 성공을 직접 보여주진 않는다 — 이어지는
/// 처리는 deposit_charge_success_screen.dart 참고.
class DepositChargeScreen extends ConsumerStatefulWidget {
  const DepositChargeScreen({super.key});

  @override
  ConsumerState<DepositChargeScreen> createState() =>
      _DepositChargeScreenState();
}

class _DepositChargeScreenState extends ConsumerState<DepositChargeScreen> {
  final _amountController = TextEditingController();

  @override
  void dispose() {
    _amountController.dispose();
    super.dispose();
  }

  num? get _amount => num.tryParse(_amountController.text.trim());

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(depositChargeViewModelProvider);
    final textTheme = Theme.of(context).textTheme;

    ref.listen(depositChargeViewModelProvider, (previous, next) {
      if (!next.hasError) return;
      if (kDebugMode) {
        debugPrint('[deposit_charge] ${next.error}\n${next.stackTrace}');
      }
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(SnackBar(content: Text(next.error.toString())));
    });

    return Scaffold(
      appBar: AppBar(title: const Text('예치금 충전')),
      body: Padding(
        padding: const EdgeInsets.all(AppSpacing.md),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text('충전 금액', style: textTheme.labelSmall),
            const SizedBox(height: AppSpacing.xs),
            TextField(
              controller: _amountController,
              enabled: !state.isLoading,
              keyboardType: TextInputType.number,
              decoration: const InputDecoration(
                hintText: '10000',
                suffixText: '원',
              ),
              onChanged: (_) => setState(() {}),
            ),
            const SizedBox(height: AppSpacing.sm),
            Wrap(
              spacing: AppSpacing.xs,
              children: [
                for (final quickAmount in const [10000, 30000, 50000, 100000])
                  ActionChip(
                    label: Text('${quickAmount ~/ 10000}만원'),
                    onPressed: state.isLoading
                        ? null
                        : () => setState(
                            () => _amountController.text = '$quickAmount',
                          ),
                  ),
              ],
            ),
            const SizedBox(height: AppSpacing.lg),
            ElevatedButton(
              onPressed: state.isLoading || _amount == null || _amount! <= 0
                  ? null
                  : () => ref
                        .read(depositChargeViewModelProvider.notifier)
                        .charge(_amount!),
              child: state.isLoading
                  ? const SizedBox(
                      width: 20,
                      height: 20,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : const Text('카드로 충전하기'),
            ),
          ],
        ),
      ),
    );
  }
}
