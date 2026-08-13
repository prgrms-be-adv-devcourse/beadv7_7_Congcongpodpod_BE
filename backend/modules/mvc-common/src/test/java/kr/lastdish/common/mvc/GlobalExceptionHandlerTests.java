package kr.lastdish.common.mvc;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.common.api.exception.CommonErrorCode;
import kr.lastdish.common.api.tracing.RequestIdSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

class GlobalExceptionHandlerTests {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  private ch.qos.logback.classic.Logger logger;
  private ListAppender<ILoggingEvent> appender;

  @BeforeEach
  void attachAppender() {
    logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
    appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
  }

  @AfterEach
  void detachAppender() {
    logger.detachAppender(appender);
    appender.stop();
  }

  @Test
  @DisplayName("예상하지 못한 예외는 ERROR 레벨로 예외와 함께 기록한다")
  void logsUnexpectedExceptionWithThrowable() {
    RuntimeException exception = new RuntimeException("DB 커넥션을 얻지 못했습니다");

    handler.handleException(exception);

    List<ILoggingEvent> events = appender.list;
    assertThat(events).hasSize(1);

    ILoggingEvent event = events.getFirst();
    assertThat(event.getLevel()).isEqualTo(Level.ERROR);
    assertThat(event.getThrowableProxy()).isNotNull();
    assertThat(event.getThrowableProxy().getMessage()).isEqualTo("DB 커넥션을 얻지 못했습니다");
  }

  @Test
  @DisplayName("예상하지 못한 예외 로그에는 공통 오류 코드와 예외 클래스가 들어간다")
  void logsErrorCodeAndExceptionClass() {
    handler.handleException(new IllegalMonitorStateException("락 해제 실패"));

    ILoggingEvent event = appender.list.getFirst();

    assertThat(event.getFormattedMessage())
        .contains(CommonErrorCode.INTERNAL_ERROR.getCode())
        .contains(IllegalMonitorStateException.class.getName());
  }

  @Test
  @DisplayName("예상된 비즈니스 예외는 ERROR 로그를 남기지 않는다")
  void doesNotLogExpectedBusinessException() {
    handler.handleBusiness(new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND));

    assertThat(appender.list).isEmpty();
  }

  @Test
  @DisplayName("예상된 입력값 오류는 ERROR 로그를 남기지 않는다")
  void doesNotLogExpectedInputError() {
    handler.handleIllegalArgument(new IllegalArgumentException("수량은 1 이상이어야 합니다"));

    assertThat(appender.list).isEmpty();
  }

  @Test
  @DisplayName("MDC에 requestId가 있으면 예외 로그에 포함한다")
  void includesRequestIdInLogWhenPresentInMdc() {
    MDC.put(RequestIdSupport.KEY, "req-abc-123");

    try {
      handler.handleException(new RuntimeException("DB 커넥션을 얻지 못했습니다"));
    } finally {
      MDC.remove(RequestIdSupport.KEY);
    }

    assertThat(appender.list.getFirst().getFormattedMessage()).contains("req-abc-123");
  }

  @Test
  @DisplayName("MDC에 requestId가 없으면 unknown으로 기록한다")
  void fallsBackToUnknownWhenRequestIdMissingFromMdc() {
    MDC.remove(RequestIdSupport.KEY);

    handler.handleException(new RuntimeException("DB 커넥션을 얻지 못했습니다"));

    assertThat(appender.list.getFirst().getFormattedMessage()).contains("unknown");
  }
}
