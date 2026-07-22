package kr.lastdish.member.member.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

// core-service의 ApiResponse.fail() 응답과 동일한 모양을 유지한다 (success/data/error/timestamp).
// 두 서비스 간 별도 공유 모듈이 없어 모양만 맞춰 각자 유지한다.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(boolean success, Object data, ApiError error, Instant timestamp) {

  public static ErrorResponse of(ErrorCode errorCode) {
    return new ErrorResponse(
        false, null, new ApiError(errorCode.name(), errorCode.getMessage()), Instant.now());
  }

  public record ApiError(String code, String message) {}
}
