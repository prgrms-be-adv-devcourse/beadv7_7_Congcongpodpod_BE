package kr.lastdish.gateway.security;

import kr.lastdish.common.api.exception.ErrorCodeSpec;
import org.springframework.http.HttpStatus;

public enum GatewayErrorCode implements ErrorCodeSpec {
  INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "G001", "인증이 필요합니다."),
  ACCESS_DENIED(HttpStatus.FORBIDDEN, "G002", "접근 권한이 없습니다.");

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
