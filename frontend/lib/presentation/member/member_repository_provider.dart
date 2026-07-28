import 'package:riverpod/riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../../core/network/dio_provider.dart';
import '../../data/repository/member_repository_impl.dart';
import '../../domain/model/member.dart';
import '../../domain/repository/member_repository.dart';

part 'member_repository_provider.g.dart';

/// MemberRepository를 조립해 앱 전역에 제공한다 (store_repository_provider.dart와 같은 패턴).
@riverpod
MemberRepository memberRepository(Ref ref) {
  return MemberRepositoryImpl(dio: ref.watch(dioProvider));
}

/// 로그인한 내 정보. 체크아웃(B7)에서 전화번호를 미리 채워주는 용도로 쓴다 —
/// cart_repository_impl.dart가 memberId를 얻으려고 호출하는 것과 별개로, 화면에서
/// 직접 필요할 때(전화번호 등 표시)는 이 provider를 쓰면 된다.
@riverpod
Future<Member> myInfo(Ref ref) {
  return ref.watch(memberRepositoryProvider).getMyInfo();
}
