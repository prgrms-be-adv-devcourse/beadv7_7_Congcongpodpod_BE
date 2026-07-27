// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'auth_repository_provider.dart';

// **************************************************************************
// RiverpodGenerator
// **************************************************************************

String _$authRepositoryHash() => r'880dee765bc68afa752b930aeecb5b8f1c9e25cf';

/// AuthRepository를 조립해 앱 전역에 제공한다.
/// tokenStorage가 비동기(SharedPreferences 초기화 대기)라 이 Provider도 Future다 —
/// 쓰는 쪽에서 `await ref.read(authRepositoryProvider.future)` 로 가져온다.
/// 로그인/회원가입이 공유하므로 auth 폴더 상위에 둔다.
///
/// Copied from [authRepository].
@ProviderFor(authRepository)
final authRepositoryProvider =
    AutoDisposeFutureProvider<AuthRepository>.internal(
  authRepository,
  name: r'authRepositoryProvider',
  debugGetCreateSourceHash: const bool.fromEnvironment('dart.vm.product')
      ? null
      : _$authRepositoryHash,
  dependencies: null,
  allTransitiveDependencies: null,
);

typedef AuthRepositoryRef = AutoDisposeFutureProviderRef<AuthRepository>;
// ignore_for_file: type=lint
// ignore_for_file: subtype_of_sealed_class, invalid_use_of_internal_member, invalid_use_of_visible_for_testing_member
