import 'package:freezed_annotation/freezed_annotation.dart';

part 'member.freezed.dart';
part 'member.g.dart';

/// 내 회원 정보. 백엔드 `MemberProfileResponse`(`GET /members/me`)와 대응:
/// `{ id, userName, name, phone, email, role, createdAt, updatedAt }`.
///
/// 지금은 `id`(memberId)만 실제로 쓰인다 — Cart 조회(`GET /carts/members/{memberId}`)에
/// memberId가 필요한데 프론트가 로그인 응답만으로는 memberId를 몰라서, 로그인 후
/// 이 API를 한 번 호출해 얻어온다(`cart_repository_impl.dart` 참고). 나머지 필드는
/// 마이페이지(B12) 화면이 실제로 만들어지면 그때 쓰인다.
@freezed
class Member with _$Member {
  const factory Member({
    required int id,
    required String userName,
    required String name,
    required String phone,
    required String email,
    required String role,
    required String createdAt,
    required String updatedAt,
  }) = _Member;

  factory Member.fromJson(Map<String, Object?> json) => _$MemberFromJson(json);
}
