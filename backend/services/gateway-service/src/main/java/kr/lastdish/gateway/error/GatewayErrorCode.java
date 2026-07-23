package kr.lastdish.gateway.error;

import kr.lastdish.common.api.exception.ErrorCodeSpec;
import org.springframework.http.HttpStatus;

/**
 * Gateway에서 직접 발생하는 오류 코드.
 *
 * <p>각 하위 서비스의 도메인 오류 코드는 해당 서비스가 관리하며, Gateway는 인증·라우팅·연결 오류만 관리한다.
 */
public enum GatewayErrorCode implements ErrorCodeSpec {
  INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "G001", "인증이 필요합니다."),
  ACCESS_DENIED(HttpStatus.FORBIDDEN, "G002", "접근 권한이 없습니다."),
  INVALID_REQUEST(HttpStatus.BAD_REQUEST, "G003", "잘못된 요청입니다."),
  ROUTE_NOT_FOUND(HttpStatus.NOT_FOUND, "G004", "요청 경로를 찾을 수 없습니다."),
  BAD_GATEWAY(HttpStatus.BAD_GATEWAY, "G502", "하위 서비스 응답 처리에 실패했습니다."),
  SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "G503", "일시적으로 서비스를 이용할 수 없습니다."),
  INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "G500", "Gateway 오류가 발생했습니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;

  GatewayErrorCode(HttpStatus status, String code, String message) {
    this.status = status;
    this.code = code;
    this.message = message;
  }

  @Override
  public HttpStatus getStatus() {
    return status;
  }

  @Override
  public String getCode() {
    return code;
  }

  @Override
  public String getMessage() {
    return message;
  }
}
