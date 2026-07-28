/// Repository/DataSource가 던지는 타입 있는 예외.
/// Riverpod AsyncNotifier의 `AsyncValue.guard()`가 잡아 AsyncError로 감싸고,
/// UI는 `.when(error: ...)`에서 이 타입으로 분기해 사용자 메시지를 보여준다.
/// (`Result<D, E>` 래퍼는 쓰지 않기로 함 — AsyncValue와 역할 중복이라 제외.)
sealed class AppException implements Exception {
  const AppException(this.message);

  final String message;

  @override
  String toString() => message;
}

final class NetworkException extends AppException {
  const NetworkException([super.message = '네트워크 연결을 확인해 주세요']);
}

final class UnauthorizedException extends AppException {
  const UnauthorizedException([super.message = '로그인이 필요합니다']);
}

/// 백엔드 에러코드(C001, D003 등)를 그대로 들고 있는 예외.
/// core-service는 {code, message} 형태 에러바디를 주지만, member-service(Auth)는
/// 아직 통일 안 됨 — 두 경우 다 이 타입으로 정규화해서 상위 레이어는 신경 안 쓰게 한다.
final class ServerException extends AppException {
  const ServerException(super.message, {this.code});

  final String? code;
}

final class UnknownException extends AppException {
  const UnknownException([super.message = '알 수 없는 문제가 발생했습니다']);
}
