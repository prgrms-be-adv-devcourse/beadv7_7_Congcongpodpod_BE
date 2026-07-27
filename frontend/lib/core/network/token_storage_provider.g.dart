// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'token_storage_provider.dart';

// **************************************************************************
// RiverpodGenerator
// **************************************************************************

String _$sharedPreferencesHash() => r'48e60558ea6530114ea20ea03e69b9fb339ab129';

/// See also [sharedPreferences].
@ProviderFor(sharedPreferences)
final sharedPreferencesProvider = FutureProvider<SharedPreferences>.internal(
  sharedPreferences,
  name: r'sharedPreferencesProvider',
  debugGetCreateSourceHash: const bool.fromEnvironment('dart.vm.product')
      ? null
      : _$sharedPreferencesHash,
  dependencies: null,
  allTransitiveDependencies: null,
);

typedef SharedPreferencesRef = FutureProviderRef<SharedPreferences>;
String _$tokenStorageHash() => r'3b3d79a8ce620c21ad544efaa5bb0ea58a9fb649';

/// 구현 교체 지점: SharedPrefsTokenStorage → SecureTokenStorage로 바꿀 때
/// 이 한 줄만 바꾸면 됨 (TokenStorage 인터페이스 뒤에 숨겨둔 이유).
///
/// Copied from [tokenStorage].
@ProviderFor(tokenStorage)
final tokenStorageProvider = FutureProvider<TokenStorage>.internal(
  tokenStorage,
  name: r'tokenStorageProvider',
  debugGetCreateSourceHash:
      const bool.fromEnvironment('dart.vm.product') ? null : _$tokenStorageHash,
  dependencies: null,
  allTransitiveDependencies: null,
);

typedef TokenStorageRef = FutureProviderRef<TokenStorage>;
// ignore_for_file: type=lint
// ignore_for_file: subtype_of_sealed_class, invalid_use_of_internal_member, invalid_use_of_visible_for_testing_member
