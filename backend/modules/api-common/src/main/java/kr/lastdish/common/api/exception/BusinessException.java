package kr.lastdish.common.api.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

  private final ErrorCodeSpec errorCode;

  public BusinessException(ErrorCodeSpec errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
  }

  public BusinessException(ErrorCodeSpec errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }
}
