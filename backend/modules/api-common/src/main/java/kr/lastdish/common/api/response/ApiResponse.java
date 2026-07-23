package kr.lastdish.common.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, T data, ApiError error, Instant timestamp) {

  public static <T> ApiResponse<T> ok(T data) {
    return new ApiResponse<>(true, data, null, Instant.now());
  }

  public static ApiResponse<Void> ok() {
    return new ApiResponse<>(true, null, null, Instant.now());
  }

  public static ApiResponse<Void> fail(String code, String message) {
    return new ApiResponse<>(false, null, new ApiError(code, message), Instant.now());
  }

  public static ApiResponse<Void> fail(ApiError error) {
    return new ApiResponse<>(false, null, error, Instant.now());
  }

  public record ApiError(String code, String message) {}
}
