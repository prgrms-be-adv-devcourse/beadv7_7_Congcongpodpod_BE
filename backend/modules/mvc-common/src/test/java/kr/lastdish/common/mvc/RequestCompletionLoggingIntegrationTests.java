package kr.lastdish.common.mvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import kr.lastdish.common.api.tracing.RequestIdSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * 필터를 따로 떼어내지 않고, 실제 자동 설정과 실제 DispatcherServlet 위에서 완료 로그를 확인한다.
 *
 * <p>단위 테스트는 경로 패턴 attribute를 손으로 넣어 흉내내므로, 프레임워크가 실제로 그 값을 채우는지는 증명하지 못한다.
 */
class RequestCompletionLoggingIntegrationTests {

  private ch.qos.logback.classic.Logger logger;
  private ListAppender<ILoggingEvent> appender;

  @BeforeEach
  void 로그수집기를_붙인다() {
    logger =
        (ch.qos.logback.classic.Logger)
            LoggerFactory.getLogger(RequestCompletionLoggingFilter.class);
    appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
  }

  @AfterEach
  void 로그수집기를_뗀다() {
    logger.detachAppender(appender);
    appender.stop();
    MDC.clear();
  }

  @Test
  void 자동설정이_요청번호_필터와_완료로그_필터를_모두_등록한다() {
    new WebApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(MvcCommonAutoConfiguration.class))
        .run(
            context ->
                assertThat(context)
                    .hasSingleBean(RequestIdFilter.class)
                    .hasSingleBean(RequestCompletionLoggingFilter.class));
  }

  @Test
  void 실제_요청에서_핸들러_경로_패턴과_요청번호가_기록된다() throws Exception {
    MockMvc mockMvc = 서버를_띄운다();

    mockMvc
        .perform(
            get("/api/v1/orders/8817342")
                .header(RequestIdSupport.HEADER_NAME, "req-276-integration"))
        .andExpect(status().isOk());

    assertThat(appender.list).hasSize(1);
    assertThat(appender.list.getFirst().getMDCPropertyMap())
        .containsEntry(RequestIdSupport.KEY, "req-276-integration");
    assertThat(appender.list.getFirst().getFormattedMessage())
        .contains("method=GET")
        .contains("pathPattern=/api/v1/orders/{orderId}")
        .contains("status=200")
        .doesNotContain("8817342");
  }

  @Test
  void 예상하지_못한_예외는_실제_상태_500으로_기록된다() throws Exception {
    MockMvc mockMvc = 서버를_띄운다();

    mockMvc.perform(get("/api/v1/orders/8817342/boom")).andExpect(status().isInternalServerError());

    assertThat(appender.list).hasSize(1);
    assertThat(appender.list.getFirst().getFormattedMessage())
        .contains("pathPattern=/api/v1/orders/{orderId}/boom")
        .contains("status=500");
  }

  private MockMvc 서버를_띄운다() {
    return MockMvcBuilders.standaloneSetup(new 시험용주문컨트롤러())
        .setControllerAdvice(new GlobalExceptionHandler())
        .addFilters(new RequestIdFilter(), new RequestCompletionLoggingFilter(true))
        .build();
  }

  @RestController
  static class 시험용주문컨트롤러 {

    @GetMapping("/api/v1/orders/{orderId}")
    String 주문을_조회한다(@PathVariable("orderId") String orderId) {
      return orderId;
    }

    @GetMapping("/api/v1/orders/{orderId}/boom")
    String 예상하지_못한_예외를_던진다() {
      throw new RuntimeException("예상하지 못한 오류");
    }
  }
}
