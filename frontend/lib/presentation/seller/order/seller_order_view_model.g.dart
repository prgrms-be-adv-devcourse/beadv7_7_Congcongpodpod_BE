// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'seller_order_view_model.dart';

// **************************************************************************
// RiverpodGenerator
// **************************************************************************

String _$sellerOrderListHash() => r'fed1f605735cfea8ed9f4ad06221a54e165ca8b7';

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

/// 매장 주문 목록(S3). order_list_view_model.dart(구매자용)과 같은 구조 —
/// 상태 필터는 별도 Provider로 관리하고, 이 Provider가 그 값을 구독해서
/// 필터가 바뀌면 자동으로 다시 조회한다.
///
/// Copied from [sellerOrderList].
@ProviderFor(sellerOrderList)
const sellerOrderListProvider = SellerOrderListFamily();

/// 매장 주문 목록(S3). order_list_view_model.dart(구매자용)과 같은 구조 —
/// 상태 필터는 별도 Provider로 관리하고, 이 Provider가 그 값을 구독해서
/// 필터가 바뀌면 자동으로 다시 조회한다.
///
/// Copied from [sellerOrderList].
class SellerOrderListFamily extends Family<AsyncValue<List<Order>>> {
  /// 매장 주문 목록(S3). order_list_view_model.dart(구매자용)과 같은 구조 —
  /// 상태 필터는 별도 Provider로 관리하고, 이 Provider가 그 값을 구독해서
  /// 필터가 바뀌면 자동으로 다시 조회한다.
  ///
  /// Copied from [sellerOrderList].
  const SellerOrderListFamily();

  /// 매장 주문 목록(S3). order_list_view_model.dart(구매자용)과 같은 구조 —
  /// 상태 필터는 별도 Provider로 관리하고, 이 Provider가 그 값을 구독해서
  /// 필터가 바뀌면 자동으로 다시 조회한다.
  ///
  /// Copied from [sellerOrderList].
  SellerOrderListProvider call(
    int storeId,
  ) {
    return SellerOrderListProvider(
      storeId,
    );
  }

  @override
  SellerOrderListProvider getProviderOverride(
    covariant SellerOrderListProvider provider,
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
  String? get name => r'sellerOrderListProvider';
}

/// 매장 주문 목록(S3). order_list_view_model.dart(구매자용)과 같은 구조 —
/// 상태 필터는 별도 Provider로 관리하고, 이 Provider가 그 값을 구독해서
/// 필터가 바뀌면 자동으로 다시 조회한다.
///
/// Copied from [sellerOrderList].
class SellerOrderListProvider extends AutoDisposeFutureProvider<List<Order>> {
  /// 매장 주문 목록(S3). order_list_view_model.dart(구매자용)과 같은 구조 —
  /// 상태 필터는 별도 Provider로 관리하고, 이 Provider가 그 값을 구독해서
  /// 필터가 바뀌면 자동으로 다시 조회한다.
  ///
  /// Copied from [sellerOrderList].
  SellerOrderListProvider(
    int storeId,
  ) : this._internal(
          (ref) => sellerOrderList(
            ref as SellerOrderListRef,
            storeId,
          ),
          from: sellerOrderListProvider,
          name: r'sellerOrderListProvider',
          debugGetCreateSourceHash:
              const bool.fromEnvironment('dart.vm.product')
                  ? null
                  : _$sellerOrderListHash,
          dependencies: SellerOrderListFamily._dependencies,
          allTransitiveDependencies:
              SellerOrderListFamily._allTransitiveDependencies,
          storeId: storeId,
        );

  SellerOrderListProvider._internal(
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
    FutureOr<List<Order>> Function(SellerOrderListRef provider) create,
  ) {
    return ProviderOverride(
      origin: this,
      override: SellerOrderListProvider._internal(
        (ref) => create(ref as SellerOrderListRef),
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
  AutoDisposeFutureProviderElement<List<Order>> createElement() {
    return _SellerOrderListProviderElement(this);
  }

  @override
  bool operator ==(Object other) {
    return other is SellerOrderListProvider && other.storeId == storeId;
  }

  @override
  int get hashCode {
    var hash = _SystemHash.combine(0, runtimeType.hashCode);
    hash = _SystemHash.combine(hash, storeId.hashCode);

    return _SystemHash.finish(hash);
  }
}

mixin SellerOrderListRef on AutoDisposeFutureProviderRef<List<Order>> {
  /// The parameter `storeId` of this provider.
  int get storeId;
}

class _SellerOrderListProviderElement
    extends AutoDisposeFutureProviderElement<List<Order>>
    with SellerOrderListRef {
  _SellerOrderListProviderElement(super.provider);

  @override
  int get storeId => (origin as SellerOrderListProvider).storeId;
}

String _$selectedSellerOrderStatusHash() =>
    r'cd550f226a302f002a52f758b620fd202871b53f';

/// S3 상태 필터 칩이 선택한 값. `null`이면 전체.
///
/// Copied from [SelectedSellerOrderStatus].
@ProviderFor(SelectedSellerOrderStatus)
final selectedSellerOrderStatusProvider =
    AutoDisposeNotifierProvider<SelectedSellerOrderStatus, String?>.internal(
  SelectedSellerOrderStatus.new,
  name: r'selectedSellerOrderStatusProvider',
  debugGetCreateSourceHash: const bool.fromEnvironment('dart.vm.product')
      ? null
      : _$selectedSellerOrderStatusHash,
  dependencies: null,
  allTransitiveDependencies: null,
);

typedef _$SelectedSellerOrderStatus = AutoDisposeNotifier<String?>;
String _$sellerOrderActionViewModelHash() =>
    r'544663335fa8c1adeaee328e5e3dddbc3154a044';

/// 접수/거절/픽업완료/노쇼처리 제출 상태 — 화면이 성공을 감지하면
/// `sellerOrderListProvider(storeId)`를 무효화해서 목록을 새로 그린다.
///
/// Copied from [SellerOrderActionViewModel].
@ProviderFor(SellerOrderActionViewModel)
final sellerOrderActionViewModelProvider =
    AutoDisposeAsyncNotifierProvider<SellerOrderActionViewModel, int?>.internal(
  SellerOrderActionViewModel.new,
  name: r'sellerOrderActionViewModelProvider',
  debugGetCreateSourceHash: const bool.fromEnvironment('dart.vm.product')
      ? null
      : _$sellerOrderActionViewModelHash,
  dependencies: null,
  allTransitiveDependencies: null,
);

typedef _$SellerOrderActionViewModel = AutoDisposeAsyncNotifier<int?>;
// ignore_for_file: type=lint
// ignore_for_file: subtype_of_sealed_class, invalid_use_of_internal_member, invalid_use_of_visible_for_testing_member
