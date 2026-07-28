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
}
