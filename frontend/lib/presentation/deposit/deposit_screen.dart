import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/routing/route_paths.dart';
import '../../domain/model/deposit.dart';
import '../../ui/app_colors.dart';
import '../../ui/app_spacing.dart';
import 'deposit_providers.dart';

const _depositTypeLabels = {'CHARGE': '충전', 'USE': '사용', 'REFUND': '환불'};

/// 예치금 잔액/내역 화면 (B11, `/deposits`). 2026-07-27 재구현: `GET /deposits/balance`,
/// `GET /deposits/history` 실제 연동.
class DepositScreen extends ConsumerWidget {
  const DepositScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final balanceAsync = ref.watch(depositBalanceProvider);
    final historyAsync = ref.watch(depositHistoryProvider);
    final textTheme = Theme.of(context).textTheme;

    return Scaffold(
      appBar: AppBar(title: const Text('예치금')),
      body: RefreshIndicator(
        onRefresh: () async {
          ref.invalidate(depositBalanceProvider);
          ref.invalidate(depositHistoryProvider);
          await Future.wait([
            ref.read(depositBalanceProvider.future),
            ref.read(depositHistoryProvider.future),
          ]);
        },
        child: ListView(
          padding: const EdgeInsets.all(AppSpacing.md),
          children: [
            Card(
              child: Padding(
                padding: const EdgeInsets.all(AppSpacing.lg),
                child: Column(
                  children: [
                    Text('잔액', style: textTheme.labelSmall),
                    const SizedBox(height: AppSpacing.xs),
                    balanceAsync.when(
                      data: (balance) => Text(
                        '${balance.balance.toInt()}원',
                        style: textTheme.headlineMedium?.copyWith(
                          color: AppColors.primary,
                          fontWeight: FontWeight.w800,
                        ),
                      ),
                      error: (error, _) => Text(error.toString()),
                      loading: () => const CircularProgressIndicator(),
                    ),
                    const SizedBox(height: AppSpacing.md),
                    OutlinedButton(
                      onPressed: () => context.push(RoutePaths.depositCharge),
                      child: const Text('충전하기'),
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: AppSpacing.lg),
            Text('내역', style: textTheme.titleMedium),
            const SizedBox(height: AppSpacing.sm),
            historyAsync.when(
              data: (history) {
                if (history.isEmpty) {
                  return const Padding(
                    padding: EdgeInsets.symmetric(vertical: AppSpacing.lg),
                    child: Center(
                      child: Text('내역이 없어요', style: TextStyle(color: AppColors.textHint)),
                    ),
                  );
                }
                return Column(
                  children: [
                    for (final entry in history) _HistoryTile(entry: entry),
                  ],
                );
              },
              error: (error, _) => Padding(
                padding: const EdgeInsets.symmetric(vertical: AppSpacing.lg),
                child: Text(error.toString(), textAlign: TextAlign.center),
              ),
              loading: () => const Padding(
                padding: EdgeInsets.symmetric(vertical: AppSpacing.lg),
                child: Center(child: CircularProgressIndicator()),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _HistoryTile extends StatelessWidget {
  const _HistoryTile({required this.entry});

  final DepositHistoryEntry entry;

  @override
  Widget build(BuildContext context) {
    final isUse = entry.type == 'USE';
    final sign = isUse ? '-' : '+';
    return Card(
      child: ListTile(
        title: Text(_depositTypeLabels[entry.type] ?? entry.type),
        subtitle: Text(entry.createdAt.replaceFirst('T', ' ')),
        trailing: Text(
          '$sign${entry.amount.toInt()}원',
          style: TextStyle(
            color: isUse ? AppColors.error : AppColors.primary,
            fontWeight: FontWeight.w700,
          ),
        ),
      ),
    );
  }
}
