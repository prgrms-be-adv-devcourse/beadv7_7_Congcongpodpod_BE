package kr.lastdish.common.mvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import kr.lastdish.common.api.tracing.RequestIdSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

class RequestCompletionLoggingFilterTests {

  private final RequestCompletionLoggingFilter filter = new RequestCompletionLoggingFilter();

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
  void 정상_요청은_INFO_완료_로그를_한_줄_남긴다() throws Exception {
    MockHttpServletRequest request = 요청("GET", "/api/v1/orders/8817342");
    request.setAttribute(
        HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/v1/orders/{orderId}");

    filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

    assertThat(appender.list).hasSize(1);
    assertThat(appender.list.getFirst().getLevel()).isEqualTo(Level.INFO);
  }

  @Test
  void 완료_로그에_요청번호와_메서드와_경로패턴과_상태와_처리시간을_남긴다() throws Exception {
    MDC.put(RequestIdSupport.KEY, "req-276-001");
    MockHttpServletRequest request = 요청("GET", "/api/v1/orders/8817342");
    request.setAttribute(
        HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/v1/orders/{orderId}");

    filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

    assertThat(appender.list.getFirst().getMDCPropertyMap())
        .containsEntry(RequestIdSupport.KEY, "req-276-001");
    assertThat(appender.list.getFirst().getFormattedMessage())
        .contains("method=GET")
        .contains("pathPattern=/api/v1/orders/{orderId}")
        .contains("status=200")
        .containsPattern("durationMs=\\d+");
  }

  @Test
  void 정상적인_상태확인_요청은_완료_로그를_남기지_않는다() throws Exception {
    MockHttpServletRequest request = 요청("GET", "/actuator/health");
    request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/actuator/health/**");

    filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

    assertThat(appender.list).isEmpty();
  }

  @Test
  void 정상적인_메트릭_수집_요청도_완료_로그를_남기지_않는다() throws Exception {
    MockHttpServletRequest request = 요청("GET", "/actuator/prometheus");
    request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/actuator/prometheus");

    filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

    assertThat(appender.list).isEmpty();
  }

  @Test
  void 상태확인이_실패하면_완료_로그를_남긴다() throws Exception {
    MockHttpServletRequest request = 요청("GET", "/actuator/health");
    request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/actuator/health/**");

    FilterChain 오백삼응답체인 = (req, res) -> ((HttpServletResponse) res).setStatus(503);

    filter.doFilter(request, new MockHttpServletResponse(), 오백삼응답체인);

    assertThat(appender.list).hasSize(1);
    assertThat(appender.list.getFirst().getFormattedMessage())
        .contains("pathPattern=/actuator/health/**")
        .contains("status=503");
  }

  @Test
  void 사백번대_응답도_INFO_완료_로그로만_남긴다() throws Exception {
    MockHttpServletRequest request = 요청("POST", "/api/v1/orders");
    request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/v1/orders");

    FilterChain 사백응답체인 = (req, res) -> ((HttpServletResponse) res).setStatus(400);

    filter.doFilter(request, new MockHttpServletResponse(), 사백응답체인);

    assertThat(appender.list).hasSize(1);
    assertThat(appender.list.getFirst().getLevel()).isEqualTo(Level.INFO);
    assertThat(appender.list.getFirst().getFormattedMessage()).contains("status=400");
  }

  @Test
  void 핸들러_패턴이_없으면_unmatched를_남긴다() throws Exception {
    MockHttpServletRequest request = 요청("GET", "/존재하지-않는-경로");

    filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

    assertThat(appender.list.getFirst().getFormattedMessage())
        .contains("pathPattern=unmatched")
        .doesNotContain("/존재하지-않는-경로");
  }

  @Test
  void 실제_식별자와_쿼리스트링은_로그에_남기지_않는다() throws Exception {
    MockHttpServletRequest request = 요청("GET", "/api/v1/orders/8817342");
    request.setQueryString("token=super-secret&email=someone@example.com");
    request.setAttribute(
        HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/v1/orders/{orderId}");

    filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

    assertThat(appender.list.getFirst().getFormattedMessage())
        .doesNotContain("8817342")
        .doesNotContain("token")
        .doesNotContain("super-secret")
        .doesNotContain("someone@example.com");
  }

  @Test
  void MDC에_요청번호가_없으면_unknown을_남긴다() throws Exception {
    MockHttpServletRequest request = 요청("GET", "/api/v1/orders");
    request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/v1/orders");

    filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

    assertThat(appender.list.getFirst().getMDCPropertyMap())
        .doesNotContainKey(RequestIdSupport.KEY);
  }

  @Test
  void 체인에서_예외가_전파되면_완료_로그를_남기지_않는다() {
    MockHttpServletRequest request = 요청("GET", "/api/v1/orders");

    FilterChain 예외체인 =
        (req, res) -> {
          throw new IllegalStateException("chain failure");
        };

    assertThatThrownBy(() -> filter.doFilter(request, new MockHttpServletResponse(), 예외체인))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("chain failure");

    assertThat(appender.list).isEmpty();
  }

  @Test
  void RequestIdFilter보다_안쪽에서_실행되도록_순서값이_더_크다() {
    assertThat(filter.getOrder()).isGreaterThan(new RequestIdFilter().getOrder());
  }

  private MockHttpServletRequest 요청(String method, String uri) {
    return new MockHttpServletRequest(method, uri);
  }
}
