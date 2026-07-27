// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'order_list_view_model.dart';

// **************************************************************************
// RiverpodGenerator
// **************************************************************************

String _$orderListViewModelHash() =>
    r'34634b20cd2b889cd14614db76699d697608010c';

/// 내 주문 목록 화면의 상태. store_list_view_model.dart와 같은 패턴 —
/// `ref.watch(selectedOrderStatusProvider)`로 상태 필터 탭을 구독해서, 탭이 바뀌면
/// 자동으로 다시 조회한다.
///
/// Copied from [OrderListViewModel].
@ProviderFor(OrderListViewModel)
final orderListViewModelProvider =
    AutoDisposeAsyncNotifierProvider<OrderListViewModel, List<Order>>.internal(
  OrderListViewModel.new,
  name: r'orderListViewModelProvider',
  debugGetCreateSourceHash: const bool.fromEnvironment('dart.vm.product')
      ? null
      : _$orderListViewModelHash,
  dependencies: null,
  allTransitiveDependencies: null,
);

typedef _$OrderListViewModel = AutoDisposeAsyncNotifier<List<Order>>;
String _$selectedOrderStatusHash() =>
    r'd7b8ef55255b3c0c103f13fdc141250ee3c27426';

/// 주문 목록 상태 필터 탭이 선택한 값. `null`이면 "전체".
///
/// Copied from [SelectedOrderStatus].
@ProviderFor(SelectedOrderStatus)
final selectedOrderStatusProvider =
    AutoDisposeNotifierProvider<SelectedOrderStatus, String?>.internal(
  SelectedOrderStatus.new,
  name: r'selectedOrderStatusProvider',
  debugGetCreateSourceHash: const bool.fromEnvironment('dart.vm.product')
      ? null
      : _$selectedOrderStatusHash,
  dependencies: null,
  allTransitiveDependencies: null,
);

typedef _$SelectedOrderStatus = AutoDisposeNotifier<String?>;
// ignore_for_file: type=lint
// ignore_for_file: subtype_of_sealed_class, invalid_use_of_internal_member, invalid_use_of_visible_for_testing_member
