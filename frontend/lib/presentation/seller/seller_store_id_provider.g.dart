// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'seller_store_id_provider.dart';

// **************************************************************************
// RiverpodGenerator
// **************************************************************************

String _$sellerStoreIdHash() => r'0a5098f753f7d0ee266685758121d9d5c3f30c78';

/// 로그인한 셀러의 storeId. `GET /stores/mine`(2026-07-27 백엔드 반영,
/// adr-draft-seller-store-id-resolution.md 참고)으로 직접 얻는다 — 셀러당 매장
/// 1개를 가정하고 첫 번째 것만 쓴다(여러 개면 나중에 선택 UI 필요, 지금은 범위 밖).
/// 매장이 아직 없으면(신규 셀러) null — S2/S3가 이 값으로 "먼저 매장 등록" 안내를 보여준다.
///
/// 이전엔 로컬(SharedPreferences) 캐싱이었으나, 저 API가 실제로 나와서 걷어냈다 —
/// S1(매장 등록) 성공 시 이 provider를 invalidate하면 새로 등록된 매장이 바로 반영된다.
///
/// ⚠️ `GET /stores/mine`은 SELLER 전용(Gateway 라우팅 규칙)인데, 이 provider는
/// "아직 SELLER가 아닌 사용자가 첫 매장을 등록하러 온" 시나리오(S1 진입)에서도
/// 호출된다 — 그 경우 401/403(또는 CORS 프리플라이트가 막혀 NetworkException으로
/// 보이는 경우까지)이 나는 게 정상이다. "매장 등록 시 서버가 SELLER를 자동
/// 부여한다"는 전제상 이 실패를 "매장 없음"과 동일하게 취급해 등록 폼으로
/// 보내야 한다 — 그러지 않으면 첫 매장 등록 자체가 막힌다(2026-07-29 발견).
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
