package kr.lastdish.member.member.exception;

public class InvalidTokenException extends BusinessException {

  public InvalidTokenException() {
    super(ErrorCode.INVALID_TOKEN);
  }

  public InvalidTokenException(String message) {
    super(ErrorCode.INVALID_TOKEN);
  }

  public InvalidTokenException(ErrorCode errorCode) {
    super(errorCode);
  }
}
