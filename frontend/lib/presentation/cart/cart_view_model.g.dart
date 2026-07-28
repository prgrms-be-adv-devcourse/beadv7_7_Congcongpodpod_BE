// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'cart_view_model.dart';

// **************************************************************************
// RiverpodGenerator
// **************************************************************************

String _$cartViewModelHash() => r'78702e6c32d252ade92958f5b46e9baba1bfc8ce';

/// 장바구니 화면의 상태. store_list_view_model.dart와 같은 이유로 `AsyncNotifier<Cart>` —
/// 화면에 데이터(Cart) 자체가 필요해서 build()가 조회 결과를 그대로 상태로 들고 있는다.
///
/// 수량변경/삭제/비우기 이후에 카트를 "부분적으로" 다시 계산하지 않고, 매번 서버에서
/// 통째로 다시 받아온다 — 장바구니가 상품 1개뿐이라 그 비용이
/// 아주 작아서, 굳이 로컬에서 subtotal/totalPrice를 다시 계산하는 로직을 만들지 않았다
/// (틀리기 쉬운 계산을 서버에 맡기는 게 더 안전하기도 하다).
///
/// Copied from [CartViewModel].
@ProviderFor(CartViewModel)
final cartViewModelProvider =
    AutoDisposeAsyncNotifierProvider<CartViewModel, Cart>.internal(
  CartViewModel.new,
  name: r'cartViewModelProvider',
  debugGetCreateSourceHash: const bool.fromEnvironment('dart.vm.product')
      ? null
      : _$cartViewModelHash,
  dependencies: null,
  allTransitiveDependencies: null,
);

typedef _$CartViewModel = AutoDisposeAsyncNotifier<Cart>;
// ignore_for_file: type=lint
// ignore_for_file: subtype_of_sealed_class, invalid_use_of_internal_member, invalid_use_of_visible_for_testing_member
