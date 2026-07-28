import 'package:dio/dio.dart';

import '../domain/error/app_exception.dart';

/// core-service(Cart/Dish/Store/Order/Deposit) 전용 에러 변환 함수.
///
/// core-service는 에러가 나면 항상 아래 모양으로 응답한다:
/// ```json
/// { "success": false, "data": null, "error": { "code": "D003", "message": "재고가 부족합니다" }, "timestamp": "..." }
/// ```
/// repository는 dio 호출을 try/on DioException으로 감싸고, 잡은 예외를 이 함수에 그대로 넘기기만 하면
/// 우리 앱 공통 타입인 [AppException]으로 바뀐 값이 나온다. 화면(presentation)은 dio를 몰라도 된다 —
/// auth_repository_impl.dart와 같은 원칙.
///
/// ⚠️ auth(member-service)는 이 함수를 쓰지 않는다. member-service는 아직 `{error:{code,message}}`
/// 포맷을 안 쓰고, 상태코드만 보고 판단해야 하기 때문에 auth_repository_impl.dart는
/// 지금처럼 자체 분기를 그대로 쓴다. Cart/Dish/Store/Order 쪽 repository만 이 함수를 재사용하면 된다.
///
/// 에러코드 표는 백엔드가 기능을 추가하면서 계속 바뀔 수 있는 값이라,
/// 여기서 코드마다 새 메시지를 만들어 하드코딩하지 않는다. 서버가 내려준 `message`를 그대로 보여주고,
/// `code`는 화면에서 특별한 분기가 필요할 때(예: 품절이면 다른 버튼을 보여준다든지) 참고용으로만 들고 있는다.
/// 그래서 코드 목록이 새로 늘어나거나 바뀌어도 이 함수는 고칠 필요가 없다.
AppException mapCoreServiceError(DioException e) {
  final response = e.response;

  // response가 아예 없다 = 서버에 연결 자체를 못 했거나(오프라인) 타임아웃.
  // 이 경우엔 애초에 core-service 응답 바디라는 게 없으므로 바로 네트워크 에러로 처리.
  if (response == null) {
    return const NetworkException();
  }

  final status = response.statusCode;
  final body = response.data;

  // core-service 에러 바디가 맞는지(= { error: { code, message } } 형태인지) 확인한다.
  // dio는 JSON 응답을 자동으로 Map으로 파싱해주므로 여기선 캐스팅만 하면 된다.
  if (body is Map<String, dynamic>) {
    final error = body['error'];
    if (error is Map<String, dynamic>) {
      final code = error['code'] as String?;
      final message = error['message'] as String?;
      return ServerException(message ?? '요청을 처리할 수 없습니다', code: code);
    }
  }

  // 여기까지 왔다는 건 core-service의 에러 포맷이 아니라는 뜻 —
  // 예를 들어 Gateway가 인증 단계에서 자체적으로 내려주는 401/403 같은 경우.
  if (status == 401 || status == 403) {
    return const UnauthorizedException();
  }

  // 그 외(예상 못 한 모양의 에러 바디)는 상태코드만이라도 남겨서 디버깅에 쓸 수 있게 한다.
  return ServerException('요청 중 문제가 발생했습니다', code: status?.toString());
}
