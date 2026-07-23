package kr.lastdish.common.mvc;

import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.common.api.exception.CommonErrorCode;
import kr.lastdish.common.api.exception.ErrorCodeSpec;
import kr.lastdish.common.api.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException exception) {
    ErrorCodeSpec errorCode = exception.getErrorCode();

    return build(errorCode, exception.getMessage());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(
      IllegalArgumentException exception) {
    return build(CommonErrorCode.INVALID_INPUT, exception.getMessage());
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException exception) {
    return build(CommonErrorCode.INVALID_STATE, exception.getMessage());
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class,
    MissingServletRequestParameterException.class
  })
  public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception exception) {
    return build(CommonErrorCode.INVALID_INPUT, "요청 형식이 올바르지 않습니다.");
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleValidation(
      MethodArgumentNotValidException exception) {
    String message =
        exception.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(FieldError::getDefaultMessage)
            .orElse("입력값이 올바르지 않습니다.");

    return build(CommonErrorCode.INVALID_INPUT, message);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleException(Exception exception) {
    return build(CommonErrorCode.INTERNAL_ERROR, CommonErrorCode.INTERNAL_ERROR.getMessage());
  }

  private ResponseEntity<ApiResponse<Void>> build(ErrorCodeSpec errorCode, String message) {
    return ResponseEntity.status(errorCode.getStatus())
        .body(ApiResponse.fail(errorCode.getCode(), message));
  }
}
