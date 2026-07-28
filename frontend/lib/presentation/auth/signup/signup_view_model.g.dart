// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'signup_view_model.dart';

// **************************************************************************
// RiverpodGenerator
// **************************************************************************

String _$signupViewModelHash() => r'2ff1ca96ea936cce9783ed946c0cbe03a01ed9dd';

/// 회원가입 화면의 상태(로딩/성공/에러)를 들고 있는 ViewModel.
///
/// build()가 void를 반환하므로 상태 타입은 `AsyncValue<void>` —
/// "데이터 값" 자체는 없고 로딩/성공/실패 3단계만 중요하기 때문.
/// login()이 성공하면 AsyncData(null), 실패하면 AsyncError가 되고,
/// 화면은 그 전이를 ref.listen으로 감지해 라우팅/스낵바를 처리한다.
///
/// Copied from [SignupViewModel].
@ProviderFor(SignupViewModel)
final signupViewModelProvider =
    AutoDisposeAsyncNotifierProvider<SignupViewModel, void>.internal(
  SignupViewModel.new,
  name: r'signupViewModelProvider',
  debugGetCreateSourceHash: const bool.fromEnvironment('dart.vm.product')
      ? null
      : _$signupViewModelHash,
  dependencies: null,
  allTransitiveDependencies: null,
);

typedef _$SignupViewModel = AutoDisposeAsyncNotifier<void>;
// ignore_for_file: type=lint
// ignore_for_file: subtype_of_sealed_class, invalid_use_of_internal_member, invalid_use_of_visible_for_testing_member
