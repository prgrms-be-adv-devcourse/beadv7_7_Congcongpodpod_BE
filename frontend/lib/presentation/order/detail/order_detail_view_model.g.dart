// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'order_detail_view_model.dart';

// **************************************************************************
// RiverpodGenerator
// **************************************************************************

String _$orderDetailViewModelHash() =>
    r'9f32ff658f6abb87f29b6a1a048e2863fc36e503';

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

/// 주문 단건 조회. store_detail_view_model.dart와 같은 이유로 함수형(family) Provider —
/// orderId 하나로 결과가 정해지는 순수 조회라 커맨드형 Notifier가 필요 없다.
///
/// Copied from [orderDetailViewModel].
@ProviderFor(orderDetailViewModel)
const orderDetailViewModelProvider = OrderDetailViewModelFamily();

/// 주문 단건 조회. store_detail_view_model.dart와 같은 이유로 함수형(family) Provider —
/// orderId 하나로 결과가 정해지는 순수 조회라 커맨드형 Notifier가 필요 없다.
///
/// Copied from [orderDetailViewModel].
class OrderDetailViewModelFamily extends Family<AsyncValue<Order>> {
  /// 주문 단건 조회. store_detail_view_model.dart와 같은 이유로 함수형(family) Provider —
  /// orderId 하나로 결과가 정해지는 순수 조회라 커맨드형 Notifier가 필요 없다.
  ///
  /// Copied from [orderDetailViewModel].
  const OrderDetailViewModelFamily();

  /// 주문 단건 조회. store_detail_view_model.dart와 같은 이유로 함수형(family) Provider —
  /// orderId 하나로 결과가 정해지는 순수 조회라 커맨드형 Notifier가 필요 없다.
  ///
  /// Copied from [orderDetailViewModel].
  OrderDetailViewModelProvider call(
    int orderId,
  ) {
    return OrderDetailViewModelProvider(
      orderId,
    );
  }

  @override
  OrderDetailViewModelProvider getProviderOverride(
    covariant OrderDetailViewModelProvider provider,
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
  String? get name => r'orderDetailViewModelProvider';
}

/// 주문 단건 조회. store_detail_view_model.dart와 같은 이유로 함수형(family) Provider —
/// orderId 하나로 결과가 정해지는 순수 조회라 커맨드형 Notifier가 필요 없다.
///
/// Copied from [orderDetailViewModel].
class OrderDetailViewModelProvider extends AutoDisposeFutureProvider<Order> {
  /// 주문 단건 조회. store_detail_view_model.dart와 같은 이유로 함수형(family) Provider —
  /// orderId 하나로 결과가 정해지는 순수 조회라 커맨드형 Notifier가 필요 없다.
  ///
  /// Copied from [orderDetailViewModel].
  OrderDetailViewModelProvider(
    int orderId,
  ) : this._internal(
          (ref) => orderDetailViewModel(
            ref as OrderDetailViewModelRef,
            orderId,
          ),
          from: orderDetailViewModelProvider,
          name: r'orderDetailViewModelProvider',
          debugGetCreateSourceHash:
              const bool.fromEnvironment('dart.vm.product')
                  ? null
                  : _$orderDetailViewModelHash,
          dependencies: OrderDetailViewModelFamily._dependencies,
          allTransitiveDependencies:
              OrderDetailViewModelFamily._allTransitiveDependencies,
          orderId: orderId,
        );

  OrderDetailViewModelProvider._internal(
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
    FutureOr<Order> Function(OrderDetailViewModelRef provider) create,
  ) {
    return ProviderOverride(
      origin: this,
      override: OrderDetailViewModelProvider._internal(
        (ref) => create(ref as OrderDetailViewModelRef),
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
  AutoDisposeFutureProviderElement<Order> createElement() {
    return _OrderDetailViewModelProviderElement(this);
  }

  @override
  bool operator ==(Object other) {
    return other is OrderDetailViewModelProvider && other.orderId == orderId;
  }

  @override
  int get hashCode {
    var hash = _SystemHash.combine(0, runtimeType.hashCode);
    hash = _SystemHash.combine(hash, orderId.hashCode);

    return _SystemHash.finish(hash);
  }
}

mixin OrderDetailViewModelRef on AutoDisposeFutureProviderRef<Order> {
  /// The parameter `orderId` of this provider.
  int get orderId;
}

class _OrderDetailViewModelProviderElement
    extends AutoDisposeFutureProviderElement<Order>
    with OrderDetailViewModelRef {
  _OrderDetailViewModelProviderElement(super.provider);

  @override
  int get orderId => (origin as OrderDetailViewModelProvider).orderId;
}
// ignore_for_file: type=lint
// ignore_for_file: subtype_of_sealed_class, invalid_use_of_internal_member, invalid_use_of_visible_for_testing_member
