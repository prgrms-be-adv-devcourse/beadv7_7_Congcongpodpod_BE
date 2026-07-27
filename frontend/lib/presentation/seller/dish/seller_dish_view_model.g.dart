// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'seller_dish_view_model.dart';

// **************************************************************************
// RiverpodGenerator
// **************************************************************************

String _$sellerDishHash() => r'78de3f866d46736419fc5a89c619a69d0de48675';

/// Copied from Dart SDK
class _SystemHash {
  _SystemHash._();

  static int combine(int hash, int value) {
    // ignore: parameter_assignments
    hash = 0x1fffffff & (hash + value);
    // ignore: parameter_assignments
    hash = 0x1fffffff & (hash + ((0x0007ffff & hash) << 10));
    return hash ^ (hash >> 6);
  }

  static int finish(int hash) {
    // ignore: parameter_assignments
    hash = 0x1fffffff & (hash + ((0x03ffffff & hash) << 3));
    // ignore: parameter_assignments
    hash = hash ^ (hash >> 11);
    return 0x1fffffff & (hash + ((0x00003fff & hash) << 15));
  }
}

/// 매장의 상품(S2). 매장:상품이 1:1(ADR 004)이라 단건 — `GET /stores/{storeId}/dish`
/// (2026-07-27 백엔드 신규, 이슈 #121)로 판매 상태 무관하게 조회한다. 예전엔
/// `GET /dishes?storeId=`(ON_SALE만 반환)를 썼는데 품절/마감 상품이 관리 화면에서
/// 사라지는 문제가 있어서 이걸로 교체함 — store_repository.dart의 `getMyDish` 주석 참고.
///
/// Copied from [sellerDish].
@ProviderFor(sellerDish)
const sellerDishProvider = SellerDishFamily();

/// 매장의 상품(S2). 매장:상품이 1:1(ADR 004)이라 단건 — `GET /stores/{storeId}/dish`
/// (2026-07-27 백엔드 신규, 이슈 #121)로 판매 상태 무관하게 조회한다. 예전엔
/// `GET /dishes?storeId=`(ON_SALE만 반환)를 썼는데 품절/마감 상품이 관리 화면에서
/// 사라지는 문제가 있어서 이걸로 교체함 — store_repository.dart의 `getMyDish` 주석 참고.
///
/// Copied from [sellerDish].
class SellerDishFamily extends Family<AsyncValue<Dish?>> {
  /// 매장의 상품(S2). 매장:상품이 1:1(ADR 004)이라 단건 — `GET /stores/{storeId}/dish`
  /// (2026-07-27 백엔드 신규, 이슈 #121)로 판매 상태 무관하게 조회한다. 예전엔
  /// `GET /dishes?storeId=`(ON_SALE만 반환)를 썼는데 품절/마감 상품이 관리 화면에서
  /// 사라지는 문제가 있어서 이걸로 교체함 — store_repository.dart의 `getMyDish` 주석 참고.
  ///
  /// Copied from [sellerDish].
  const SellerDishFamily();

  /// 매장의 상품(S2). 매장:상품이 1:1(ADR 004)이라 단건 — `GET /stores/{storeId}/dish`
  /// (2026-07-27 백엔드 신규, 이슈 #121)로 판매 상태 무관하게 조회한다. 예전엔
  /// `GET /dishes?storeId=`(ON_SALE만 반환)를 썼는데 품절/마감 상품이 관리 화면에서
  /// 사라지는 문제가 있어서 이걸로 교체함 — store_repository.dart의 `getMyDish` 주석 참고.
  ///
  /// Copied from [sellerDish].
  SellerDishProvider call(
    int storeId,
  ) {
    return SellerDishProvider(
      storeId,
    );
  }

  @override
  SellerDishProvider getProviderOverride(
    covariant SellerDishProvider provider,
  ) {
    return call(
      provider.storeId,
    );
  }

  static const Iterable<ProviderOrFamily>? _dependencies = null;

  @override
  Iterable<ProviderOrFamily>? get dependencies => _dependencies;

  static const Iterable<ProviderOrFamily>? _allTransitiveDependencies = null;

  @override
  Iterable<ProviderOrFamily>? get allTransitiveDependencies =>
      _allTransitiveDependencies;

  @override
  String? get name => r'sellerDishProvider';
}

/// 매장의 상품(S2). 매장:상품이 1:1(ADR 004)이라 단건 — `GET /stores/{storeId}/dish`
/// (2026-07-27 백엔드 신규, 이슈 #121)로 판매 상태 무관하게 조회한다. 예전엔
/// `GET /dishes?storeId=`(ON_SALE만 반환)를 썼는데 품절/마감 상품이 관리 화면에서
/// 사라지는 문제가 있어서 이걸로 교체함 — store_repository.dart의 `getMyDish` 주석 참고.
///
/// Copied from [sellerDish].
class SellerDishProvider extends AutoDisposeFutureProvider<Dish?> {
  /// 매장의 상품(S2). 매장:상품이 1:1(ADR 004)이라 단건 — `GET /stores/{storeId}/dish`
  /// (2026-07-27 백엔드 신규, 이슈 #121)로 판매 상태 무관하게 조회한다. 예전엔
  /// `GET /dishes?storeId=`(ON_SALE만 반환)를 썼는데 품절/마감 상품이 관리 화면에서
  /// 사라지는 문제가 있어서 이걸로 교체함 — store_repository.dart의 `getMyDish` 주석 참고.
  ///
  /// Copied from [sellerDish].
  SellerDishProvider(
    int storeId,
  ) : this._internal(
          (ref) => sellerDish(
            ref as SellerDishRef,
            storeId,
          ),
          from: sellerDishProvider,
          name: r'sellerDishProvider',
          debugGetCreateSourceHash:
              const bool.fromEnvironment('dart.vm.product')
                  ? null
                  : _$sellerDishHash,
          dependencies: SellerDishFamily._dependencies,
          allTransitiveDependencies:
              SellerDishFamily._allTransitiveDependencies,
          storeId: storeId,
        );

  SellerDishProvider._internal(
    super._createNotifier, {
    required super.name,
    required super.dependencies,
    required super.allTransitiveDependencies,
    required super.debugGetCreateSourceHash,
    required super.from,
    required this.storeId,
  }) : super.internal();

  final int storeId;

  @override
  Override overrideWith(
    FutureOr<Dish?> Function(SellerDishRef provider) create,
  ) {
    return ProviderOverride(
      origin: this,
      override: SellerDishProvider._internal(
        (ref) => create(ref as SellerDishRef),
        from: from,
        name: null,
        dependencies: null,
        allTransitiveDependencies: null,
        debugGetCreateSourceHash: null,
        storeId: storeId,
      ),
    );
  }

  @override
  AutoDisposeFutureProviderElement<Dish?> createElement() {
    return _SellerDishProviderElement(this);
  }

  @override
  bool operator ==(Object other) {
    return other is SellerDishProvider && other.storeId == storeId;
  }

  @override
  int get hashCode {
    var hash = _SystemHash.combine(0, runtimeType.hashCode);
    hash = _SystemHash.combine(hash, storeId.hashCode);

    return _SystemHash.finish(hash);
  }
}

mixin SellerDishRef on AutoDisposeFutureProviderRef<Dish?> {
  /// The parameter `storeId` of this provider.
  int get storeId;
}

class _SellerDishProviderElement extends AutoDisposeFutureProviderElement<Dish?>
    with SellerDishRef {
  _SellerDishProviderElement(super.provider);

  @override
  int get storeId => (origin as SellerDishProvider).storeId;
}

String _$sellerDishActionViewModelHash() =>
    r'392a75652c1cbd3b4360a9da9494e1e351499451';

/// 상품 등록/수정/상태변경 제출 상태 — checkout_view_model.dart와 같은 커맨드형 패턴.
/// 화면이 성공을 감지하면 `sellerDishProvider(storeId)`를 무효화해서 다시 그린다.
///
/// Copied from [SellerDishActionViewModel].
@ProviderFor(SellerDishActionViewModel)
final sellerDishActionViewModelProvider =
    AutoDisposeAsyncNotifierProvider<SellerDishActionViewModel, Dish?>.internal(
  SellerDishActionViewModel.new,
  name: r'sellerDishActionViewModelProvider',
  debugGetCreateSourceHash: const bool.fromEnvironment('dart.vm.product')
      ? null
      : _$sellerDishActionViewModelHash,
  dependencies: null,
  allTransitiveDependencies: null,
);

typedef _$SellerDishActionViewModel = AutoDisposeAsyncNotifier<Dish?>;
// ignore_for_file: type=lint
// ignore_for_file: subtype_of_sealed_class, invalid_use_of_internal_member, invalid_use_of_visible_for_testing_member
