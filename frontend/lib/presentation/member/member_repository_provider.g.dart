// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'member_repository_provider.dart';

// **************************************************************************
// RiverpodGenerator
// **************************************************************************

String _$memberRepositoryHash() => r'a23db041e30bf687a4e5303f4ecec71619c3ab70';

/// MemberRepository를 조립해 앱 전역에 제공한다 (store_repository_provider.dart와 같은 패턴).
///
/// Copied from [memberRepository].
@ProviderFor(memberRepository)
final memberRepositoryProvider = AutoDisposeProvider<MemberRepository>.internal(
  memberRepository,
  name: r'memberRepositoryProvider',
  debugGetCreateSourceHash: const bool.fromEnvironment('dart.vm.product')
      ? null
      : _$memberRepositoryHash,
  dependencies: null,
  allTransitiveDependencies: null,
);

typedef MemberRepositoryRef = AutoDisposeProviderRef<MemberRepository>;
String _$myInfoHash() => r'c197ba79f558b6a27140dc8c6b1f46c327feecc9';

/// 로그인한 내 정보. 체크아웃(B7)에서 전화번호를 미리 채워주는 용도로 쓴다 —
/// cart_repository_impl.dart가 memberId를 얻으려고 호출하는 것과 별개로, 화면에서
/// 직접 필요할 때(전화번호 등 표시)는 이 provider를 쓰면 된다.
///
/// Copied from [myInfo].
@ProviderFor(myInfo)
final myInfoProvider = AutoDisposeFutureProvider<Member>.internal(
  myInfo,
  name: r'myInfoProvider',
  debugGetCreateSourceHash:
      const bool.fromEnvironment('dart.vm.product') ? null : _$myInfoHash,
  dependencies: null,
  allTransitiveDependencies: null,
);

typedef MyInfoRef = AutoDisposeFutureProviderRef<Member>;
// ignore_for_file: type=lint
// ignore_for_file: subtype_of_sealed_class, invalid_use_of_internal_member, invalid_use_of_visible_for_testing_member
