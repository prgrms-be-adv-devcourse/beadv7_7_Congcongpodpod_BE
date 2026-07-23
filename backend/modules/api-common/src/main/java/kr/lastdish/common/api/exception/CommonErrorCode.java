package kr.lastdish.common.api.exception;

import org.springframework.http.HttpStatus;

public enum CommonErrorCode implements ErrorCodeSpec {
  INVALID_INPUT(HttpStatus.BAD_REQUEST, "C001", "잘못된 요청입니다."),
  INVALID_STATE(HttpStatus.CONFLICT, "C002", "처리할 수 없는 상태입니다."),
  ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "C003", "대상을 찾을 수 없습니다."),
  SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "C503", "일시적으로 서비스를 이용할 수 없습니다."),
  INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C500", "서버 오류가 발생했습니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;

  CommonErrorCode(HttpStatus status, String code, String message) {
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
