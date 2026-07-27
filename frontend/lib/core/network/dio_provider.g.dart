// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'dio_provider.dart';

// **************************************************************************
// RiverpodGenerator
// **************************************************************************

String _$dioHash() => r'8f717e0da1a7bd1b93d934db6157582c44c7e75d';

/// Gateway 진입점 하나만 호출한다 —
/// Member/Core 서비스 포트로 직접 호출 금지.
/// TODO: Android 에뮬레이터는 localhost가 아니라 10.0.2.2로 접근해야 함 —
/// 실기기/에뮬레이터에서 실제로 붙여볼 때 플랫폼별 baseUrl 분기 필요할 수 있음.
///
/// Copied from [dio].
@ProviderFor(dio)
final dioProvider = Provider<Dio>.internal(
  dio,
  name: r'dioProvider',
  debugGetCreateSourceHash: const bool.fromEnvironment('dart.vm.product')
      ? null
      : _$dioHash,
  dependencies: null,
  allTransitiveDependencies: null,
);

typedef DioRef = ProviderRef<Dio>;
// ignore_for_file: type=lint
// ignore_for_file: subtype_of_sealed_class, invalid_use_of_internal_member, invalid_use_of_visible_for_testing_member
