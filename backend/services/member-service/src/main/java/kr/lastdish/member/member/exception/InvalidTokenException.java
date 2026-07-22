package kr.lastdish.member.member.exception;

public class InvalidTokenException extends BusinessException {

  // 기본 메시지를 사용하는 경우 (ErrorCode.INVALID_TOKEN 활용)
  public InvalidTokenException() {
    super(ErrorCode.INVALID_TOKEN);
  }

  // 동적인 메시지나 커스텀 에러 코드가 필요한 경우를 위한 생성자
  public InvalidTokenException(String message) {
    super(ErrorCode.INVALID_TOKEN); // 필요에 따라 커스텀 에러코드를 매핑할 수도 있습니다.
  }

  public InvalidTokenException(ErrorCode errorCode) {
    super(errorCode);
  }
}
