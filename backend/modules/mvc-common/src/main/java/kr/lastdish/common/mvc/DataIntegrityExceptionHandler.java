package kr.lastdish.common.mvc;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import kr.lastdish.common.api.exception.CommonErrorCode;
import kr.lastdish.common.api.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 데이터 제약 위반 예외를 {@link GlobalExceptionHandler}보다 먼저 잡아, 위반한 값이 로그에 남지 않게 한다.
 *
 * <p>PostgreSQL은 제약을 어겼을 때 어긋난 값을 메시지에 그대로 담는다. 실제 확인한 모양은 다음과 같다.
 *
 * <pre>
 * ERROR:  duplicate key value violates unique constraint "members_email_key"
 * DETAIL:  Key (email)=(someone@example.com) already exists.
 * </pre>
 *
 * <p>이 예외에는 전용 핸들러가 없어 {@code @ExceptionHandler(Exception.class)}로 떨어졌고, 그 핸들러가 예외 객체를 그대로 넘기는 탓에
 * 이미 가입된 이메일로 가입을 시도하면 그 이메일이 ERROR 로그에 남았다. 그래서 메시지 전체 대신 <b>제약 이름만</b> 뽑아 남기고 예외 객체는 넘기지 않는다.
 *
 * <p>스택 트레이스를 잃는 대신 값이 새지 않는 쪽을 택했다. 응답은 기존과 동일하게 유지해 API 계약은 바뀌지 않는다.
 */
@RestControllerAdvice
// GlobalExceptionHandler의 Exception 핸들러보다 먼저 선택되어야 한다.
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DataIntegrityExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(DataIntegrityExceptionHandler.class);

  /** 제약 이름만 골라낸다. 큰따옴표로 둘러싸인 부분만 취하므로 위반한 값은 따라오지 않는다. */
  private static final Pattern CONSTRAINT_NAME = Pattern.compile("constraint \"([^\"]+)\"");

  /** 제약 이름을 찾지 못했을 때 남기는 값. 메시지 원문으로 대체하지 않는다. */
  private static final String UNKNOWN_CONSTRAINT = "unknown";

  /** 위반한 값 대신 제약 이름만 남기고, 응답은 기존 서버 오류와 동일하게 돌려준다. */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(
      DataIntegrityViolationException exception) {
    log.error(
        "데이터 제약 조건을 위반했습니다. constraint={}, errorCode={}, exceptionClass={}",
        resolveConstraintName(exception),
        CommonErrorCode.INTERNAL_ERROR.getCode(),
        exception.getClass().getName());

    return ResponseEntity.status(CommonErrorCode.INTERNAL_ERROR.getStatus())
        .body(
            ApiResponse.fail(
                CommonErrorCode.INTERNAL_ERROR.getCode(),
                CommonErrorCode.INTERNAL_ERROR.getMessage()));
  }

  /** 메시지에서 첫 번째 제약 이름만 취한다. 못 찾으면 원문을 남기지 않고 unknown으로 둔다. */
  private String resolveConstraintName(DataIntegrityViolationException exception) {
    String message = exception.getMessage();

    if (message == null) {
      return UNKNOWN_CONSTRAINT;
    }

    Matcher matcher = CONSTRAINT_NAME.matcher(message);
    return matcher.find() ? matcher.group(1) : UNKNOWN_CONSTRAINT;
  }
}
