import 'package:riverpod/riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../../core/network/dio_provider.dart';
import '../../data/repository/cart_repository_impl.dart';
import '../../domain/repository/cart_repository.dart';
import '../member/member_repository_provider.dart';

part 'cart_repository_provider.g.dart';

/// CartRepository를 조립해 앱 전역에 제공한다 (store_repository_provider.dart와 같은 패턴).
/// memberId 확보용으로 MemberRepository도 같이 주입한다(cart_repository_impl.dart 참고).
@riverpod
CartRepository cartRepository(Ref ref) {
  return CartRepositoryImpl(
    dio: ref.watch(dioProvider),
    memberRepository: ref.watch(memberRepositoryProvider),
  );
}
