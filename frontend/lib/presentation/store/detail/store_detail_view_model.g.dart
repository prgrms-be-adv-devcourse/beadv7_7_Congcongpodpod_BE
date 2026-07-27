// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'store_detail_view_model.dart';

// **************************************************************************
// RiverpodGenerator
// **************************************************************************

String _$storeDetailViewModelHash() =>
    r'eba9a43343aa20a4c6df4c85b6ec75dc5359f107';

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

/// 매장 상세 조회. 로그인/목록과 달리 클래스형 Notifier가 아니라 "함수형" Provider다 —
/// 커맨드(로그인, 새로고침 같은 동작)가 없고 storeId 하나로 결과가 정해지는 순수 조회라서
/// 이 편이 더 단순하다. 코드젠이 매개변수(storeId)를 보고 자동으로 "family"
/// (storeId마다 상태를 따로 캐싱하는 Provider)를 만들어준다 — 매장 A 상세를 보다가
/// 매장 B 상세로 가면 서로 다른 결과가 각자 캐싱된다.
///
/// 화면에서는 `ref.watch(storeDetailViewModelProvider(storeId))`처럼 파라미터를 넣어서 쓴다.
///
/// Copied from [storeDetailViewModel].
@ProviderFor(storeDetailViewModel)
const storeDetailViewModelProvider = StoreDetailViewModelFamily();

/// 매장 상세 조회. 로그인/목록과 달리 클래스형 Notifier가 아니라 "함수형" Provider다 —
/// 커맨드(로그인, 새로고침 같은 동작)가 없고 storeId 하나로 결과가 정해지는 순수 조회라서
/// 이 편이 더 단순하다. 코드젠이 매개변수(storeId)를 보고 자동으로 "family"
/// (storeId마다 상태를 따로 캐싱하는 Provider)를 만들어준다 — 매장 A 상세를 보다가
/// 매장 B 상세로 가면 서로 다른 결과가 각자 캐싱된다.
///
/// 화면에서는 `ref.watch(storeDetailViewModelProvider(storeId))`처럼 파라미터를 넣어서 쓴다.
///
/// Copied from [storeDetailViewModel].
class StoreDetailViewModelFamily extends Family<AsyncValue<Store>> {
  /// 매장 상세 조회. 로그인/목록과 달리 클래스형 Notifier가 아니라 "함수형" Provider다 —
  /// 커맨드(로그인, 새로고침 같은 동작)가 없고 storeId 하나로 결과가 정해지는 순수 조회라서
  /// 이 편이 더 단순하다. 코드젠이 매개변수(storeId)를 보고 자동으로 "family"
  /// (storeId마다 상태를 따로 캐싱하는 Provider)를 만들어준다 — 매장 A 상세를 보다가
  /// 매장 B 상세로 가면 서로 다른 결과가 각자 캐싱된다.
  ///
  /// 화면에서는 `ref.watch(storeDetailViewModelProvider(storeId))`처럼 파라미터를 넣어서 쓴다.
  ///
  /// Copied from [storeDetailViewModel].
  const StoreDetailViewModelFamily();

  /// 매장 상세 조회. 로그인/목록과 달리 클래스형 Notifier가 아니라 "함수형" Provider다 —
  /// 커맨드(로그인, 새로고침 같은 동작)가 없고 storeId 하나로 결과가 정해지는 순수 조회라서
  /// 이 편이 더 단순하다. 코드젠이 매개변수(storeId)를 보고 자동으로 "family"
  /// (storeId마다 상태를 따로 캐싱하는 Provider)를 만들어준다 — 매장 A 상세를 보다가
  /// 매장 B 상세로 가면 서로 다른 결과가 각자 캐싱된다.
  ///
  /// 화면에서는 `ref.watch(storeDetailViewModelProvider(storeId))`처럼 파라미터를 넣어서 쓴다.
  ///
  /// Copied from [storeDetailViewModel].
  StoreDetailViewModelProvider call(
    int storeId,
  ) {
    return StoreDetailViewModelProvider(
      storeId,
    );
  }

  @override
  StoreDetailViewModelProvider getProviderOverride(
    covariant StoreDetailViewModelProvider provider,
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
  String? get name => r'storeDetailViewModelProvider';
}

/// 매장 상세 조회. 로그인/목록과 달리 클래스형 Notifier가 아니라 "함수형" Provider다 —
/// 커맨드(로그인, 새로고침 같은 동작)가 없고 storeId 하나로 결과가 정해지는 순수 조회라서
/// 이 편이 더 단순하다. 코드젠이 매개변수(storeId)를 보고 자동으로 "family"
/// (storeId마다 상태를 따로 캐싱하는 Provider)를 만들어준다 — 매장 A 상세를 보다가
/// 매장 B 상세로 가면 서로 다른 결과가 각자 캐싱된다.
///
/// 화면에서는 `ref.watch(storeDetailViewModelProvider(storeId))`처럼 파라미터를 넣어서 쓴다.
///
/// Copied from [storeDetailViewModel].
class StoreDetailViewModelProvider extends AutoDisposeFutureProvider<Store> {
  /// 매장 상세 조회. 로그인/목록과 달리 클래스형 Notifier가 아니라 "함수형" Provider다 —
  /// 커맨드(로그인, 새로고침 같은 동작)가 없고 storeId 하나로 결과가 정해지는 순수 조회라서
  /// 이 편이 더 단순하다. 코드젠이 매개변수(storeId)를 보고 자동으로 "family"
  /// (storeId마다 상태를 따로 캐싱하는 Provider)를 만들어준다 — 매장 A 상세를 보다가
  /// 매장 B 상세로 가면 서로 다른 결과가 각자 캐싱된다.
  ///
  /// 화면에서는 `ref.watch(storeDetailViewModelProvider(storeId))`처럼 파라미터를 넣어서 쓴다.
  ///
  /// Copied from [storeDetailViewModel].
  StoreDetailViewModelProvider(
    int storeId,
  ) : this._internal(
          (ref) => storeDetailViewModel(
            ref as StoreDetailViewModelRef,
            storeId,
          ),
          from: storeDetailViewModelProvider,
          name: r'storeDetailViewModelProvider',
          debugGetCreateSourceHash:
              const bool.fromEnvironment('dart.vm.product')
                  ? null
                  : _$storeDetailViewModelHash,
          dependencies: StoreDetailViewModelFamily._dependencies,
          allTransitiveDependencies:
              StoreDetailViewModelFamily._allTransitiveDependencies,
          storeId: storeId,
        );

  StoreDetailViewModelProvider._internal(
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
    FutureOr<Store> Function(StoreDetailViewModelRef provider) create,
  ) {
    return ProviderOverride(
      origin: this,
      override: StoreDetailViewModelProvider._internal(
        (ref) => create(ref as StoreDetailViewModelRef),
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
    return _StoreDetailViewModelProviderElement(this);
  }

  @override
  bool operator ==(Object other) {
    return other is StoreDetailViewModelProvider && other.storeId == storeId;
  }

  @override
  int get hashCode {
    var hash = _SystemHash.combine(0, runtimeType.hashCode);
    hash = _SystemHash.combine(hash, storeId.hashCode);

    return _SystemHash.finish(hash);
  }
}

mixin StoreDetailViewModelRef on AutoDisposeFutureProviderRef<Store> {
  /// The parameter `storeId` of this provider.
  int get storeId;
}

class _StoreDetailViewModelProviderElement
    extends AutoDisposeFutureProviderElement<Store>
    with StoreDetailViewModelRef {
  _StoreDetailViewModelProviderElement(super.provider);

  @override
  int get storeId => (origin as StoreDetailViewModelProvider).storeId;
}
// ignore_for_file: type=lint
// ignore_for_file: subtype_of_sealed_class, invalid_use_of_internal_member, invalid_use_of_visible_for_testing_member
