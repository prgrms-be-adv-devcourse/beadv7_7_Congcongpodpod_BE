// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'deposit_providers.dart';

// **************************************************************************
// RiverpodGenerator
// **************************************************************************

String _$depositRepositoryHash() => r'77d569bf7159c3d7173acbaa64d9bff7eed5f2af';

/// DepositRepository를 조립해 앱 전역에 제공한다 (store_repository_provider.dart와 같은 패턴).
///
/// Copied from [depositRepository].
@ProviderFor(depositRepository)
final depositRepositoryProvider =
    AutoDisposeProvider<DepositRepository>.internal(
  depositRepository,
  name: r'depositRepositoryProvider',
  debugGetCreateSourceHash: const bool.fromEnvironment('dart.vm.product')
      ? null
      : _$depositRepositoryHash,
  dependencies: null,
  allTransitiveDependencies: null,
);

typedef DepositRepositoryRef = AutoDisposeProviderRef<DepositRepository>;
String _$depositBalanceHash() => r'4d4e34cd37a2b97431348d7f9eb2e5a929b2a141';

/// See also [depositBalance].
@ProviderFor(depositBalance)
final depositBalanceProvider =
    AutoDisposeFutureProvider<DepositBalance>.internal(
  depositBalance,
  name: r'depositBalanceProvider',
  debugGetCreateSourceHash: const bool.fromEnvironment('dart.vm.product')
      ? null
      : _$depositBalanceHash,
  dependencies: null,
  allTransitiveDependencies: null,
);

typedef DepositBalanceRef = AutoDisposeFutureProviderRef<DepositBalance>;
String _$depositHistoryHash() => r'c04f3b04cb8745b58d1e525ff44c7d4ff8844297';

/// See also [depositHistory].
@ProviderFor(depositHistory)
final depositHistoryProvider =
    AutoDisposeFutureProvider<List<DepositHistoryEntry>>.internal(
  depositHistory,
  name: r'depositHistoryProvider',
  debugGetCreateSourceHash: const bool.fromEnvironment('dart.vm.product')
      ? null
      : _$depositHistoryHash,
  dependencies: null,
  allTransitiveDependencies: null,
);

typedef DepositHistoryRef
    = AutoDisposeFutureProviderRef<List<DepositHistoryEntry>>;
// ignore_for_file: type=lint
// ignore_for_file: subtype_of_sealed_class, invalid_use_of_internal_member, invalid_use_of_visible_for_testing_member
