// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'deposit_charge_success_view_model.dart';

// **************************************************************************
// RiverpodGenerator
// **************************************************************************

String _$depositChargeSuccessViewModelHash() =>
    r'36b426ece6b1bd1febb68072f4a78b7fb029b3ba';

/// 충전 성공 리다이렉트 화면(B14 연장)의 상태. Toss가 넘겨준 `paymentKey`/`orderId`/
/// `amount`로 승인(`POST /payments/approve`)을 호출해 예치금 충전을 확정한다.
///
/// Copied from [DepositChargeSuccessViewModel].
@ProviderFor(DepositChargeSuccessViewModel)
final depositChargeSuccessViewModelProvider =
    AutoDisposeAsyncNotifierProvider<
      DepositChargeSuccessViewModel,
      PaymentApprove?
    >.internal(
      DepositChargeSuccessViewModel.new,
      name: r'depositChargeSuccessViewModelProvider',
      debugGetCreateSourceHash: const bool.fromEnvironment('dart.vm.product')
          ? null
          : _$depositChargeSuccessViewModelHash,
      dependencies: null,
      allTransitiveDependencies: null,
    );

typedef _$DepositChargeSuccessViewModel =
    AutoDisposeAsyncNotifier<PaymentApprove?>;
// ignore_for_file: type=lint
// ignore_for_file: subtype_of_sealed_class, invalid_use_of_internal_member, invalid_use_of_visible_for_testing_member
