// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'store_repository_provider.dart';

// **************************************************************************
// RiverpodGenerator
// **************************************************************************

String _$storeRepositoryHash() => r'a008ebd9dce0cc75b3f95fa2e976a1f61a7c0262';

/// StoreRepository를 조립해 앱 전역에 제공한다 (auth_repository_provider.dart와 같은 패턴).
/// Store 조회는 로그인 없이도 되는 공개 API라,
/// tokenStorage를 안 기다려도 돼서 auth와 달리 Future가 아니라 그냥 값을 바로 만든다.
/// 목록/상세 화면이 공유하므로 store 폴더 상위에 둔다.
///
/// Copied from [storeRepository].
@ProviderFor(storeRepository)
final storeRepositoryProvider = AutoDisposeProvider<StoreRepository>.internal(
  storeRepository,
  name: r'storeRepositoryProvider',
  debugGetCreateSourceHash: const bool.fromEnvironment('dart.vm.product')
      ? null
      : _$storeRepositoryHash,
  dependencies: null,
  allTransitiveDependencies: null,
);

typedef StoreRepositoryRef = AutoDisposeProviderRef<StoreRepository>;
// ignore_for_file: type=lint
// ignore_for_file: subtype_of_sealed_class, invalid_use_of_internal_member, invalid_use_of_visible_for_testing_member
