import '../model/member.dart';

/// 회원 정보 조회 기능 계약. 실제 dio 호출은 lib/data/repository/member_repository_impl.dart가 담당한다.
/// (store_repository.dart/cart_repository.dart와 같은 원칙 — presentation은 이 인터페이스만 알면 된다.)
abstract interface class MemberRepository {
  /// 내 정보 조회 (`GET /members/me`). 지금은 Cart가 memberId를 얻는 용도로만 쓴다
  /// (`cart_repository_impl.dart` 참고) — 마이페이지(B12)가 실제로 만들어지면 거기서도 재사용.
  Future<Member> getMyInfo();
}
