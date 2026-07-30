// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'checkout_view_model.dart';

// **************************************************************************
// RiverpodGenerator
// **************************************************************************

String _$checkoutViewModelHash() => r'67d11026d667b806e23d7146b3c54276904483f5';

/// 체크아웃 화면의 상태 — signup_view_model.dart와 비슷하지만, 성공했을 때 생성된
/// [Order]를 그대로 들고 있는다(확인 다이얼로그에 주문번호/픽업시간 등을 보여줘야 해서).
///
/// Copied from [CheckoutViewModel].
@ProviderFor(CheckoutViewModel)
final checkoutViewModelProvider =
    AutoDisposeAsyncNotifierProvider<CheckoutViewModel, Order?>.internal(
  CheckoutViewModel.new,
  name: r'checkoutViewModelProvider',
  debugGetCreateSourceHash: const bool.fromEnvironment('dart.vm.product')
      ? null
      : _$checkoutViewModelHash,
  dependencies: null,
  allTransitiveDependencies: null,
);

typedef _$CheckoutViewModel = AutoDisposeAsyncNotifier<Order?>;
// ignore_for_file: type=lint
// ignore_for_file: subtype_of_sealed_class, invalid_use_of_internal_member, invalid_use_of_visible_for_testing_member
