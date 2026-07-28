// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'store_list_view_model.dart';

// **************************************************************************
// RiverpodGenerator
// **************************************************************************

String _$storeListViewModelHash() =>
    r'4467f1662dbfe272643e66e14ee292dadffb2c65';

/// 매장 목록 화면의 상태. 로그인 ViewModel(`AsyncValue<void>`, "성공했나만 중요")과 달리
/// 여기선 데이터 자체(`List<Store>`)가 화면에 필요하다 — 그래서 `build()`가 API 결과를
/// 그대로 반환하고, `AsyncNotifier`가 그 반환값을 초기 상태로 잡아준다. 화면은
/// `ref.watch(storeListViewModelProvider)`로 이 상태를 읽고 `.when(data/error/loading)`으로 그린다.
///
/// ⚠️ 위/경도는 지금 하드코딩이다(강남역 부근 좌표) — 실기기 위치 권한 연동은
/// 범위 밖으로 미뤄뒀다.
/// 나중에 실제 위치로 바꿀 때는 이 두 상수를 GPS 값으로 교체하기만 하면 된다.
///
/// Copied from [StoreListViewModel].
@ProviderFor(StoreListViewModel)
final storeListViewModelProvider =
    AutoDisposeAsyncNotifierProvider<StoreListViewModel, List<Store>>.internal(
      StoreListViewModel.new,
      name: r'storeListViewModelProvider',
      debugGetCreateSourceHash: const bool.fromEnvironment('dart.vm.product')
          ? null
          : _$storeListViewModelHash,
      dependencies: null,
      allTransitiveDependencies: null,
    );

typedef _$StoreListViewModel = AutoDisposeAsyncNotifier<List<Store>>;
// ignore_for_file: type=lint
// ignore_for_file: subtype_of_sealed_class, invalid_use_of_internal_member, invalid_use_of_visible_for_testing_member
