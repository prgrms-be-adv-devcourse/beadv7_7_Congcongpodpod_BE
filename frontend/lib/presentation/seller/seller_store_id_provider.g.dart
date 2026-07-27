// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'seller_store_id_provider.dart';

// **************************************************************************
// RiverpodGenerator
// **************************************************************************

String _$sellerStoreIdHash() => r'ef36d7b6da1dbe13c0642f029cc914f168e0fd80';

/// 로그인한 셀러의 storeId. `GET /stores/mine`(2026-07-27 백엔드 반영,
/// adr-draft-seller-store-id-resolution.md 참고)으로 직접 얻는다 — 셀러당 매장
/// 1개를 가정하고 첫 번째 것만 쓴다(여러 개면 나중에 선택 UI 필요, 지금은 범위 밖).
/// 매장이 아직 없으면(신규 셀러) null — S2/S3가 이 값으로 "먼저 매장 등록" 안내를 보여준다.
///
/// 이전엔 로컬(SharedPreferences) 캐싱이었으나, 저 API가 실제로 나와서 걷어냈다 —
/// S1(매장 등록) 성공 시 이 provider를 invalidate하면 새로 등록된 매장이 바로 반영된다.
///
/// Copied from [sellerStoreId].
@ProviderFor(sellerStoreId)
final sellerStoreIdProvider = AutoDisposeFutureProvider<int?>.internal(
  sellerStoreId,
  name: r'sellerStoreIdProvider',
  debugGetCreateSourceHash: const bool.fromEnvironment('dart.vm.product')
      ? null
      : _$sellerStoreIdHash,
  dependencies: null,
  allTransitiveDependencies: null,
);

typedef SellerStoreIdRef = AutoDisposeFutureProviderRef<int?>;
// ignore_for_file: type=lint
// ignore_for_file: subtype_of_sealed_class, invalid_use_of_internal_member, invalid_use_of_visible_for_testing_member
