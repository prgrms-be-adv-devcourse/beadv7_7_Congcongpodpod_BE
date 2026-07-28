// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'order_pickup_view_model.dart';

// **************************************************************************
// RiverpodGenerator
// **************************************************************************

String _$orderPickupViewModelHash() =>
    r'75efbe8d9c5affe60babe3faae1ce59371b85f1a';

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

/// 픽업코드 조회. store_detail_view_model.dart와 같은 함수형(family) Provider —
/// 본인 주문이면서 픽업 가능 상태(`PICKUP_READY`)가 아니면 서버가 404를 준다
/// (order_repository.dart 참고) — 화면은 그 에러를 안내 문구로 보여주면 된다.
///
/// Copied from [orderPickupViewModel].
@ProviderFor(orderPickupViewModel)
const orderPickupViewModelProvider = OrderPickupViewModelFamily();

/// 픽업코드 조회. store_detail_view_model.dart와 같은 함수형(family) Provider —
/// 본인 주문이면서 픽업 가능 상태(`PICKUP_READY`)가 아니면 서버가 404를 준다
/// (order_repository.dart 참고) — 화면은 그 에러를 안내 문구로 보여주면 된다.
///
/// Copied from [orderPickupViewModel].
class OrderPickupViewModelFamily extends Family<AsyncValue<PickupCode>> {
  /// 픽업코드 조회. store_detail_view_model.dart와 같은 함수형(family) Provider —
  /// 본인 주문이면서 픽업 가능 상태(`PICKUP_READY`)가 아니면 서버가 404를 준다
  /// (order_repository.dart 참고) — 화면은 그 에러를 안내 문구로 보여주면 된다.
  ///
  /// Copied from [orderPickupViewModel].
  const OrderPickupViewModelFamily();

  /// 픽업코드 조회. store_detail_view_model.dart와 같은 함수형(family) Provider —
  /// 본인 주문이면서 픽업 가능 상태(`PICKUP_READY`)가 아니면 서버가 404를 준다
  /// (order_repository.dart 참고) — 화면은 그 에러를 안내 문구로 보여주면 된다.
  ///
  /// Copied from [orderPickupViewModel].
  OrderPickupViewModelProvider call(
    int orderId,
  ) {
    return OrderPickupViewModelProvider(
      orderId,
    );
  }

  @override
  OrderPickupViewModelProvider getProviderOverride(
    covariant OrderPickupViewModelProvider provider,
  ) {
    return call(
      provider.orderId,
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
  String? get name => r'orderPickupViewModelProvider';
}

/// 픽업코드 조회. store_detail_view_model.dart와 같은 함수형(family) Provider —
/// 본인 주문이면서 픽업 가능 상태(`PICKUP_READY`)가 아니면 서버가 404를 준다
/// (order_repository.dart 참고) — 화면은 그 에러를 안내 문구로 보여주면 된다.
///
/// Copied from [orderPickupViewModel].
class OrderPickupViewModelProvider
    extends AutoDisposeFutureProvider<PickupCode> {
  /// 픽업코드 조회. store_detail_view_model.dart와 같은 함수형(family) Provider —
  /// 본인 주문이면서 픽업 가능 상태(`PICKUP_READY`)가 아니면 서버가 404를 준다
  /// (order_repository.dart 참고) — 화면은 그 에러를 안내 문구로 보여주면 된다.
  ///
  /// Copied from [orderPickupViewModel].
  OrderPickupViewModelProvider(
    int orderId,
  ) : this._internal(
          (ref) => orderPickupViewModel(
            ref as OrderPickupViewModelRef,
            orderId,
          ),
          from: orderPickupViewModelProvider,
          name: r'orderPickupViewModelProvider',
          debugGetCreateSourceHash:
              const bool.fromEnvironment('dart.vm.product')
                  ? null
                  : _$orderPickupViewModelHash,
          dependencies: OrderPickupViewModelFamily._dependencies,
          allTransitiveDependencies:
              OrderPickupViewModelFamily._allTransitiveDependencies,
          orderId: orderId,
        );

  OrderPickupViewModelProvider._internal(
    super._createNotifier, {
    required super.name,
    required super.dependencies,
    required super.allTransitiveDependencies,
    required super.debugGetCreateSourceHash,
    required super.from,
    required this.orderId,
  }) : super.internal();

  final int orderId;

  @override
  Override overrideWith(
    FutureOr<PickupCode> Function(OrderPickupViewModelRef provider) create,
  ) {
    return ProviderOverride(
      origin: this,
      override: OrderPickupViewModelProvider._internal(
        (ref) => create(ref as OrderPickupViewModelRef),
        from: from,
        name: null,
        dependencies: null,
        allTransitiveDependencies: null,
        debugGetCreateSourceHash: null,
        orderId: orderId,
      ),
    );
  }

  @override
  AutoDisposeFutureProviderElement<PickupCode> createElement() {
    return _OrderPickupViewModelProviderElement(this);
  }

  @override
  bool operator ==(Object other) {
    return other is OrderPickupViewModelProvider && other.orderId == orderId;
  }

  @override
  int get hashCode {
    var hash = _SystemHash.combine(0, runtimeType.hashCode);
    hash = _SystemHash.combine(hash, orderId.hashCode);

    return _SystemHash.finish(hash);
  }
}

mixin OrderPickupViewModelRef on AutoDisposeFutureProviderRef<PickupCode> {
  /// The parameter `orderId` of this provider.
  int get orderId;
}

class _OrderPickupViewModelProviderElement
    extends AutoDisposeFutureProviderElement<PickupCode>
    with OrderPickupViewModelRef {
  _OrderPickupViewModelProviderElement(super.provider);

  @override
  int get orderId => (origin as OrderPickupViewModelProvider).orderId;
}
// ignore_for_file: type=lint
// ignore_for_file: subtype_of_sealed_class, invalid_use_of_internal_member, invalid_use_of_visible_for_testing_member
