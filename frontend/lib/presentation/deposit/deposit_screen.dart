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

// 서버가 마이크로초까지 실어 보내는 ISO 문자열(예: "2026-07-29T17:45:46.377216")을
// 초 단위까지만 잘라 사람이 읽기 편한 "7월 29일 17:45"로 바꾼다.
String _formatHistoryDate(String isoString) {
  final parsed = DateTime.tryParse(isoString);
  if (parsed == null) return isoString;
  final local = parsed.toLocal();
  final minute = local.minute.toString().padLeft(2, '0');
  return '${local.month}월 ${local.day}일 ${local.hour}:$minute';
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
        subtitle: Text(_formatHistoryDate(entry.createdAt)),
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
