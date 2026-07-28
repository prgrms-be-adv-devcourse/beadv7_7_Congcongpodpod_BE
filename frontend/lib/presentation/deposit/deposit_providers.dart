import 'package:riverpod/riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../../core/network/dio_provider.dart';
import '../../data/repository/deposit_repository_impl.dart';
import '../../domain/model/deposit.dart';
import '../../domain/repository/deposit_repository.dart';

part 'deposit_providers.g.dart';

/// DepositRepository를 조립해 앱 전역에 제공한다 (store_repository_provider.dart와 같은 패턴).
@riverpod
DepositRepository depositRepository(Ref ref) {
  return DepositRepositoryImpl(dio: ref.watch(dioProvider));
}

@riverpod
Future<DepositBalance> depositBalance(Ref ref) {
  return ref.watch(depositRepositoryProvider).getBalance();
}

@riverpod
Future<List<DepositHistoryEntry>> depositHistory(Ref ref) {
  return ref.watch(depositRepositoryProvider).getHistory();
}
