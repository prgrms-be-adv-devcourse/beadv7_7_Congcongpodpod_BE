/// 인증(로그인/회원가입) 기능 전용 에러.
/// member-service는 아직 전역 예외 처리가 통일 안 돼 있어,
/// 응답 상태코드(400/401)로만 실패를 구분한다. 그 구분 결과를 이 타입으로 정규화한다.
sealed class AuthException implements Exception {
  const AuthException(this.message);

  final String message;

  @override
  String toString() => message;
}

/// 이메일/비밀번호 불일치 (로그인 400/401).
final class InvalidCredentialsException extends AuthException {
  const InvalidCredentialsException([super.message = '이메일 또는 비밀번호가 올바르지 않습니다']);
}

/// 회원가입 실패 (400/401 — 아이디/이메일 중복 등). member-service가 에러코드를
/// 안 줘서 원인 세분화는 못 하고, 로그인 실패 메시지와는 구분만 한다.
final class SignupFailedException extends AuthException {
  const SignupFailedException([super.message = '회원가입에 실패했습니다. 입력값을 확인해 주세요']);
}
