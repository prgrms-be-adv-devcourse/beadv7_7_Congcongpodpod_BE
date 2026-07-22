package kr.lastdish.member.member.exception;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ErrorResponse {
  private final int status;
  private final String error;
  private final String code;
  private final String message;

  public static ErrorResponse of(ErrorCode errorCode) {
    return ErrorResponse.builder()
        .status(errorCode.getStatus().value())
        .error(errorCode.getStatus().getReasonPhrase())
        .code(errorCode.name())
        .message(errorCode.getMessage())
        .build();
  }
}
