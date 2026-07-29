// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'deposit_charge_view_model.dart';

// **************************************************************************
// RiverpodGenerator
// **************************************************************************

String _$depositChargeViewModelHash() =>
    r'039a262b727998623d71431c80fd236d314a7f64';

/// 예치금 충전 화면(B14)의 상태. 결제 준비(`ready`) 성공 후 Toss 결제창을 띄우는
/// 순간 브라우저가 페이지를 통째로 떠나기 때문에(성공 시 successUrl로 리다이렉트),
/// 이 Notifier가 "성공"으로 끝나는 경우는 사실상 없다 — 성공 처리는
/// deposit_charge_success_view_model.dart(승인 API 호출)가 별도 화면에서 이어받는다.
/// 여기서 잡는 에러는 결제창을 띄우기 전 단계(준비 API 실패, 결제창 자체를 못 연 경우)뿐이다.
///
/// Copied from [DepositChargeViewModel].
@ProviderFor(DepositChargeViewModel)
final depositChargeViewModelProvider =
    AutoDisposeAsyncNotifierProvider<DepositChargeViewModel, void>.internal(
      DepositChargeViewModel.new,
      name: r'depositChargeViewModelProvider',
      debugGetCreateSourceHash: const bool.fromEnvironment('dart.vm.product')
          ? null
          : _$depositChargeViewModelHash,
      dependencies: null,
      allTransitiveDependencies: null,
    );

typedef _$DepositChargeViewModel = AutoDisposeAsyncNotifier<void>;
// ignore_for_file: type=lint
// ignore_for_file: subtype_of_sealed_class, invalid_use_of_internal_member, invalid_use_of_visible_for_testing_member
