package kr.lastdish.common.mvc;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class DataIntegrityExceptionHandlerTests {

  /** 실제 PostgreSQL이 만들어내는 메시지 모양. DETAIL에 위반한 값이 그대로 들어 있다. */
  private static final String POSTGRES_MESSAGE =
      "could not execute statement ["
          + "ERROR: duplicate key value violates unique constraint \"members_email_key\"\n"
          + "  Detail: Key (email)=(someone@example.com) already exists.]";

  private final DataIntegrityExceptionHandler handler = new DataIntegrityExceptionHandler();

  private ch.qos.logback.classic.Logger logger;
  private ListAppender<ILoggingEvent> appender;

  @BeforeEach
  void 로그수집기를_붙인다() {
    logger =
        (ch.qos.logback.classic.Logger)
            LoggerFactory.getLogger(DataIntegrityExceptionHandler.class);
    appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
  }

  @AfterEach
  void 로그수집기를_뗀다() {
    logger.detachAppender(appender);
    appender.stop();
  }

  @Test
  @DisplayName("제약을 위반한 값은 로그에 남기지 않는다")
  void 위반한_값을_남기지_않는다() {
    handler.handleDataIntegrityViolation(new DataIntegrityViolationException(POSTGRES_MESSAGE));

    ILoggingEvent event = appender.list.getFirst();
    assertThat(event.getFormattedMessage())
        .doesNotContain("someone@example.com")
        .doesNotContain("Detail")
        .doesNotContain("Key (email)");
  }

  @Test
  @DisplayName("원인 조사에 필요한 제약 이름은 남긴다")
  void 제약_이름은_남긴다() {
    handler.handleDataIntegrityViolation(new DataIntegrityViolationException(POSTGRES_MESSAGE));

    ILoggingEvent event = appender.list.getFirst();
    assertThat(event.getLevel()).isEqualTo(Level.ERROR);
    assertThat(event.getFormattedMessage()).contains("members_email_key");
  }

  @Test
  @DisplayName("제약 이름을 못 찾으면 값 대신 unknown을 남긴다")
  void 제약_이름을_못_찾으면_unknown을_남긴다() {
    handler.handleDataIntegrityViolation(
        new DataIntegrityViolationException(
            "null value in column \"email\" of relation \"members\""));

    assertThat(appender.list.getFirst().getFormattedMessage())
        .contains("unknown")
        .doesNotContain("relation");
  }

  @Test
  @DisplayName("예외 객체를 그대로 넘기지 않아 스택 트레이스가 남지 않는다")
  void 스택_트레이스를_남기지_않는다() {
    handler.handleDataIntegrityViolation(new DataIntegrityViolationException(POSTGRES_MESSAGE));

    assertThat(appender.list.getFirst().getThrowableProxy()).isNull();
  }

  @Test
  @DisplayName("실제 요청에서 GlobalExceptionHandler보다 먼저 선택된다")
  void 전역_핸들러보다_먼저_선택된다() throws Exception {
    MockMvc mockMvc =
        MockMvcBuilders.standaloneSetup(new 제약위반컨트롤러())
            .setControllerAdvice(new GlobalExceptionHandler(), new DataIntegrityExceptionHandler())
            .build();

    mockMvc
        .perform(MockMvcRequestBuilders.get("/api/v1/members"))
        .andExpect(MockMvcResultMatchers.status().isInternalServerError());

    assertThat(appender.list).hasSize(1);
    assertThat(appender.list.getFirst().getFormattedMessage())
        .contains("members_email_key")
        .doesNotContain("someone@example.com");
  }

  @RestController
  static class 제약위반컨트롤러 {

    @GetMapping("/api/v1/members")
    String 가입한다() {
      throw new DataIntegrityViolationException(POSTGRES_MESSAGE);
    }
  }
}
