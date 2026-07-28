// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'dish_providers.dart';

// **************************************************************************
// RiverpodGenerator
// **************************************************************************

String _$dishRepositoryHash() => r'3c2f6425da6f9a28d0e83f1ca2528ece3dca77fc';

/// DishRepository를 조립해 앱 전역에 제공한다 (store_repository_provider.dart와 같은 패턴).
///
/// Copied from [dishRepository].
@ProviderFor(dishRepository)
final dishRepositoryProvider = AutoDisposeProvider<DishRepository>.internal(
  dishRepository,
  name: r'dishRepositoryProvider',
  debugGetCreateSourceHash: const bool.fromEnvironment('dart.vm.product')
      ? null
      : _$dishRepositoryHash,
  dependencies: null,
  allTransitiveDependencies: null,
);

typedef DishRepositoryRef = AutoDisposeProviderRef<DishRepository>;
String _$dishHash() => r'c17f848246abdc9a688710addd1be0530930ea40';

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

/// 상품 단건 조회. family라 `dishId`별로 각각 캐시된다 — 같은 상품을 여러 화면에서
/// 동시에 봐도 dishId가 같으면 API 호출은 한 번만 나간다.
///
/// Copied from [dish].
@ProviderFor(dish)
const dishProvider = DishFamily();

/// 상품 단건 조회. family라 `dishId`별로 각각 캐시된다 — 같은 상품을 여러 화면에서
/// 동시에 봐도 dishId가 같으면 API 호출은 한 번만 나간다.
///
/// Copied from [dish].
class DishFamily extends Family<AsyncValue<Dish>> {
  /// 상품 단건 조회. family라 `dishId`별로 각각 캐시된다 — 같은 상품을 여러 화면에서
  /// 동시에 봐도 dishId가 같으면 API 호출은 한 번만 나간다.
  ///
  /// Copied from [dish].
  const DishFamily();

  /// 상품 단건 조회. family라 `dishId`별로 각각 캐시된다 — 같은 상품을 여러 화면에서
  /// 동시에 봐도 dishId가 같으면 API 호출은 한 번만 나간다.
  ///
  /// Copied from [dish].
  DishProvider call(
    int dishId,
  ) {
    return DishProvider(
      dishId,
    );
  }

  @override
  DishProvider getProviderOverride(
    covariant DishProvider provider,
  ) {
    return call(
      provider.dishId,
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
  String? get name => r'dishProvider';
}

/// 상품 단건 조회. family라 `dishId`별로 각각 캐시된다 — 같은 상품을 여러 화면에서
/// 동시에 봐도 dishId가 같으면 API 호출은 한 번만 나간다.
///
/// Copied from [dish].
class DishProvider extends AutoDisposeFutureProvider<Dish> {
  /// 상품 단건 조회. family라 `dishId`별로 각각 캐시된다 — 같은 상품을 여러 화면에서
  /// 동시에 봐도 dishId가 같으면 API 호출은 한 번만 나간다.
  ///
  /// Copied from [dish].
  DishProvider(
    int dishId,
  ) : this._internal(
          (ref) => dish(
            ref as DishRef,
            dishId,
          ),
          from: dishProvider,
          name: r'dishProvider',
          debugGetCreateSourceHash:
              const bool.fromEnvironment('dart.vm.product') ? null : _$dishHash,
          dependencies: DishFamily._dependencies,
          allTransitiveDependencies: DishFamily._allTransitiveDependencies,
          dishId: dishId,
        );

  DishProvider._internal(
    super._createNotifier, {
    required super.name,
    required super.dependencies,
    required super.allTransitiveDependencies,
    required super.debugGetCreateSourceHash,
    required super.from,
    required this.dishId,
  }) : super.internal();

  final int dishId;

  @override
  Override overrideWith(
    FutureOr<Dish> Function(DishRef provider) create,
  ) {
    return ProviderOverride(
      origin: this,
      override: DishProvider._internal(
        (ref) => create(ref as DishRef),
        from: from,
        name: null,
        dependencies: null,
        allTransitiveDependencies: null,
        debugGetCreateSourceHash: null,
        dishId: dishId,
      ),
    );
  }

  @override
  AutoDisposeFutureProviderElement<Dish> createElement() {
    return _DishProviderElement(this);
  }

  @override
  bool operator ==(Object other) {
    return other is DishProvider && other.dishId == dishId;
  }

  @override
  int get hashCode {
    var hash = _SystemHash.combine(0, runtimeType.hashCode);
    hash = _SystemHash.combine(hash, dishId.hashCode);

    return _SystemHash.finish(hash);
  }
}

mixin DishRef on AutoDisposeFutureProviderRef<Dish> {
  /// The parameter `dishId` of this provider.
  int get dishId;
}

class _DishProviderElement extends AutoDisposeFutureProviderElement<Dish>
    with DishRef {
  _DishProviderElement(super.provider);

  @override
  int get dishId => (origin as DishProvider).dishId;
}
// ignore_for_file: type=lint
// ignore_for_file: subtype_of_sealed_class, invalid_use_of_internal_member, invalid_use_of_visible_for_testing_member
