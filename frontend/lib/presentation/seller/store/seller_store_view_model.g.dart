// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'seller_store_view_model.dart';

// **************************************************************************
// RiverpodGenerator
// **************************************************************************

String _$sellerStoreDetailHash() => r'21aa12d471c9ac925cd5081ad709ebb7ae050869';

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

/// 매장 단건 조회 — 이미 매장이 있는 셀러가 S1 화면에 들어왔을 때 수정 폼을
/// 미리 채우는 용도 (order_detail_view_model.dart와 같은 family 패턴).
///
/// Copied from [sellerStoreDetail].
@ProviderFor(sellerStoreDetail)
const sellerStoreDetailProvider = SellerStoreDetailFamily();

/// 매장 단건 조회 — 이미 매장이 있는 셀러가 S1 화면에 들어왔을 때 수정 폼을
/// 미리 채우는 용도 (order_detail_view_model.dart와 같은 family 패턴).
///
/// Copied from [sellerStoreDetail].
class SellerStoreDetailFamily extends Family<AsyncValue<Store>> {
  /// 매장 단건 조회 — 이미 매장이 있는 셀러가 S1 화면에 들어왔을 때 수정 폼을
  /// 미리 채우는 용도 (order_detail_view_model.dart와 같은 family 패턴).
  ///
  /// Copied from [sellerStoreDetail].
  const SellerStoreDetailFamily();

  /// 매장 단건 조회 — 이미 매장이 있는 셀러가 S1 화면에 들어왔을 때 수정 폼을
  /// 미리 채우는 용도 (order_detail_view_model.dart와 같은 family 패턴).
  ///
  /// Copied from [sellerStoreDetail].
  SellerStoreDetailProvider call(
    int storeId,
  ) {
    return SellerStoreDetailProvider(
      storeId,
    );
  }

  @override
  SellerStoreDetailProvider getProviderOverride(
    covariant SellerStoreDetailProvider provider,
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
  String? get name => r'sellerStoreDetailProvider';
}

/// 매장 단건 조회 — 이미 매장이 있는 셀러가 S1 화면에 들어왔을 때 수정 폼을
/// 미리 채우는 용도 (order_detail_view_model.dart와 같은 family 패턴).
///
/// Copied from [sellerStoreDetail].
class SellerStoreDetailProvider extends AutoDisposeFutureProvider<Store> {
  /// 매장 단건 조회 — 이미 매장이 있는 셀러가 S1 화면에 들어왔을 때 수정 폼을
  /// 미리 채우는 용도 (order_detail_view_model.dart와 같은 family 패턴).
  ///
  /// Copied from [sellerStoreDetail].
  SellerStoreDetailProvider(
    int storeId,
  ) : this._internal(
          (ref) => sellerStoreDetail(
            ref as SellerStoreDetailRef,
            storeId,
          ),
          from: sellerStoreDetailProvider,
          name: r'sellerStoreDetailProvider',
          debugGetCreateSourceHash:
              const bool.fromEnvironment('dart.vm.product')
                  ? null
                  : _$sellerStoreDetailHash,
          dependencies: SellerStoreDetailFamily._dependencies,
          allTransitiveDependencies:
              SellerStoreDetailFamily._allTransitiveDependencies,
          storeId: storeId,
        );

  SellerStoreDetailProvider._internal(
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
    FutureOr<Store> Function(SellerStoreDetailRef provider) create,
  ) {
    return ProviderOverride(
      origin: this,
      override: SellerStoreDetailProvider._internal(
        (ref) => create(ref as SellerStoreDetailRef),
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
  AutoDisposeFutureProviderElement<Store> createElement() {
    return _SellerStoreDetailProviderElement(this);
  }

  @override
  bool operator ==(Object other) {
    return other is SellerStoreDetailProvider && other.storeId == storeId;
  }

  @override
  int get hashCode {
    var hash = _SystemHash.combine(0, runtimeType.hashCode);
    hash = _SystemHash.combine(hash, storeId.hashCode);

    return _SystemHash.finish(hash);
  }
}

mixin SellerStoreDetailRef on AutoDisposeFutureProviderRef<Store> {
  /// The parameter `storeId` of this provider.
  int get storeId;
}

class _SellerStoreDetailProviderElement
    extends AutoDisposeFutureProviderElement<Store> with SellerStoreDetailRef {
  _SellerStoreDetailProviderElement(super.provider);

  @override
  int get storeId => (origin as SellerStoreDetailProvider).storeId;
}

String _$sellerStoreViewModelHash() =>
    r'81bae856996306601aa42cec466169b073a7aa63';

/// S1(매장 등록/수정) 제출 상태. checkout_view_model.dart와 같은 패턴 —
/// 성공하면 결과 [Store]를 그대로 들고 있는다(등록 직후 storeId를 캐싱해야 해서).
///
/// Copied from [SellerStoreViewModel].
@ProviderFor(SellerStoreViewModel)
final sellerStoreViewModelProvider =
    AutoDisposeAsyncNotifierProvider<SellerStoreViewModel, Store?>.internal(
  SellerStoreViewModel.new,
  name: r'sellerStoreViewModelProvider',
  debugGetCreateSourceHash: const bool.fromEnvironment('dart.vm.product')
      ? null
      : _$sellerStoreViewModelHash,
  dependencies: null,
  allTransitiveDependencies: null,
);

typedef _$SellerStoreViewModel = AutoDisposeAsyncNotifier<Store?>;
// ignore_for_file: type=lint
// ignore_for_file: subtype_of_sealed_class, invalid_use_of_internal_member, invalid_use_of_visible_for_testing_member
