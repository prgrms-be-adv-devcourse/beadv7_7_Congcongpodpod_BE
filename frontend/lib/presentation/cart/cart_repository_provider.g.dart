// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'cart_repository_provider.dart';

// **************************************************************************
// RiverpodGenerator
// **************************************************************************

String _$cartRepositoryHash() => r'ba1f98f3529805397d779d75c6e35493a6653663';

/// CartRepository를 조립해 앱 전역에 제공한다 (store_repository_provider.dart와 같은 패턴).
/// memberId 확보용으로 MemberRepository도 같이 주입한다(cart_repository_impl.dart 참고).
///
/// Copied from [cartRepository].
@ProviderFor(cartRepository)
final cartRepositoryProvider = AutoDisposeProvider<CartRepository>.internal(
  cartRepository,
  name: r'cartRepositoryProvider',
  debugGetCreateSourceHash: const bool.fromEnvironment('dart.vm.product')
      ? null
      : _$cartRepositoryHash,
  dependencies: null,
  allTransitiveDependencies: null,
);

typedef CartRepositoryRef = AutoDisposeProviderRef<CartRepository>;
// ignore_for_file: type=lint
// ignore_for_file: subtype_of_sealed_class, invalid_use_of_internal_member, invalid_use_of_visible_for_testing_member
