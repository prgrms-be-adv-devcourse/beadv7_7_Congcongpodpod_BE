/// 인증 기능 계약. 구현은 lib/data/repository/auth_repository_impl.dart.
/// 화면(presentation)은 이 인터페이스만 알고, 실제 dio 호출은 모른다.
abstract interface class AuthRepository {
  /// 로그인 성공 시 토큰을 저장소에 저장까지 마친다(반환값 없음).
  /// 실패 시 AuthException 또는 core의 AppException을 던진다.
  Future<void> login({required String email, required String password});

  Future<void> signup({
    required String userName,
    required String name,
    required String phone,
    required String email,
    required String password,
  });

  /// 저장된 refresh token으로 access/refresh token을 재발급받아 저장소를 갱신한다.
  /// role처럼 서버 DB 상태가 바뀌었는데 기존 access token엔 아직 반영 안 된 값을
  /// 다시 읽어와야 할 때 쓴다(예: 매장 등록 직후 MEMBER→SELLER 승격).
  Future<void> refresh();
}
