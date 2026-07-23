package kr.lastdish.member.member.exception;

import kr.lastdish.common.api.exception.BusinessException;

public class InvalidTokenException extends BusinessException {

  public InvalidTokenException(ErrorCode errorCode) {
    super(errorCode);
  }
}
