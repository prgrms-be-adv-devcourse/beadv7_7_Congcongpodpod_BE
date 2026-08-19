package kr.lastdish.gateway.tracing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_PREDICATE_MATCHED_PATH_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_PREDICATE_MATCHED_PATH_ROUTE_ID_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.net.URI;
import kr.lastdish.common.api.tracing.RequestIdSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

class RequestCompletionLoggerTests {

  private final RequestCompletionLogger completionLogger = new RequestCompletionLogger();

  private ch.qos.logback.classic.Logger 로거;
  private ListAppender<ILoggingEvent> appender;

  @BeforeEach
  void 로그수집기를_붙인다() {
    로거 = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(RequestCompletionLogger.class);
    appender = new ListAppender<>();
    appender.start();
    로거.addAppender(appender);
  }

  @AfterEach
  void 로그수집기를_뗀다() {
    로거.detachAppender(appender);
    appender.stop();
  }

  @Test
  void 정상_완료는_다섯_필드를_INFO로_남긴다() {
    MockServerWebExchange exchange = 요청("/api/v1/orders/8817342");
    라우트를_붙인다(exchange, "core-service", "core-service", "/api/v1/orders/**");
    exchange.getResponse().setStatusCode(HttpStatus.OK);

    completionLogger.markStarted(exchange);
    completionLogger.logCompletion(exchange);

    assertThat(appender.list).hasSize(1);
    assertThat(appender.list.getFirst().getLevel()).isEqualTo(Level.INFO);
    assertThat(appender.list.getFirst().getMDCPropertyMap())
        .containsEntry(RequestIdSupport.KEY, "req-276-gw");
    assertThat(appender.list.getFirst().getFormattedMessage())
        .contains("method=GET")
        .contains("pathPattern=/api/v1/orders/**")
        .contains("status=200")
        .containsPattern("durationMs=\\d+");
  }

  @Test
  void 최종_라우트와_경로패턴을_남긴_라우트가_다르면_unmatched를_남긴다() {
    MockServerWebExchange exchange = 요청("/api/v1/orders/8817342");
    라우트를_붙인다(exchange, "member-service", "core-service", "/api/v1/orders/**");
    exchange.getResponse().setStatusCode(HttpStatus.OK);

    completionLogger.markStarted(exchange);
    completionLogger.logCompletion(exchange);

    assertThat(appender.list.getFirst().getFormattedMessage())
        .contains("pathPattern=unmatched")
        .doesNotContain("/api/v1/orders/**");
  }

  @Test
  void 라우트가_없으면_unmatched를_남긴다() {
    MockServerWebExchange exchange = 요청("/존재하지-않는-경로");
    exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);

    completionLogger.markStarted(exchange);
    completionLogger.logCompletion(exchange);

    assertThat(appender.list.getFirst().getFormattedMessage())
        .contains("pathPattern=unmatched")
        .contains("status=404")
        .doesNotContain("/존재하지-않는-경로");
  }

  @Test
  void 같은_요청에_두_번_호출해도_한_줄만_남는다() {
    MockServerWebExchange exchange = 요청("/api/v1/orders/8817342");
    라우트를_붙인다(exchange, "core-service", "core-service", "/api/v1/orders/**");
    exchange.getResponse().setStatusCode(HttpStatus.OK);

    completionLogger.markStarted(exchange);
    completionLogger.logCompletion(exchange);
    completionLogger.logCompletion(exchange);

    assertThat(appender.list).hasSize(1);
  }

  @Test
  void 시작_표시가_없으면_기록하지_않는다() {
    MockServerWebExchange exchange = 요청("/api/v1/orders/8817342");
    exchange.getResponse().setStatusCode(HttpStatus.OK);

    completionLogger.logCompletion(exchange);

    assertThat(appender.list).isEmpty();
  }

  @Test
  void 상태코드가_정해지지_않았으면_기록하지_않는다() {
    MockServerWebExchange exchange = 요청("/api/v1/orders/8817342");

    completionLogger.markStarted(exchange);
    completionLogger.logCompletion(exchange);

    assertThat(appender.list).isEmpty();
  }

  @Test
  void 실제_식별자와_쿼리스트링은_남기지_않는다() {
    MockServerWebExchange exchange =
        요청("/api/v1/orders/8817342?token=super-secret&email=someone@example.com");
    라우트를_붙인다(exchange, "core-service", "core-service", "/api/v1/orders/**");
    exchange.getResponse().setStatusCode(HttpStatus.OK);

    completionLogger.markStarted(exchange);
    completionLogger.logCompletion(exchange);

    assertThat(appender.list.getFirst().getFormattedMessage())
        .doesNotContain("8817342")
        .doesNotContain("token")
        .doesNotContain("super-secret")
        .doesNotContain("someone@example.com");
  }

  @Test
  void 정상적인_상태확인_요청은_완료_로그를_남기지_않는다() {
    MockServerWebExchange exchange = 요청("/actuator/health");
    exchange.getResponse().setStatusCode(HttpStatus.OK);
    completionLogger.markStarted(exchange);

    completionLogger.logCompletion(exchange);

    assertThat(appender.list).isEmpty();
  }

  @Test
  void 정상적인_메트릭_수집_요청도_완료_로그를_남기지_않는다() {
    MockServerWebExchange exchange = 요청("/actuator/prometheus");
    exchange.getResponse().setStatusCode(HttpStatus.OK);
    completionLogger.markStarted(exchange);

    completionLogger.logCompletion(exchange);

    assertThat(appender.list).isEmpty();
  }

  @Test
  void 상태확인이_실패하면_완료_로그를_남긴다() {
    MockServerWebExchange exchange = 요청("/actuator/health");
    exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
    completionLogger.markStarted(exchange);

    completionLogger.logCompletion(exchange);

    assertThat(appender.list).hasSize(1);
    assertThat(appender.list.getFirst().getFormattedMessage()).contains("status=503");
  }

  @Test
  void 상태확인_제외_판단에_쓴_실제_경로는_로그에_남기지_않는다() {
    MockServerWebExchange exchange = 요청("/actuator/health");
    exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
    completionLogger.markStarted(exchange);

    completionLogger.logCompletion(exchange);

    assertThat(appender.list.getFirst().getFormattedMessage()).doesNotContain("/actuator/health");
  }

  private MockServerWebExchange 요청(String uri) {
    MockServerWebExchange exchange =
        MockServerWebExchange.from(MockServerHttpRequest.get(uri).build());
    exchange.getAttributes().put(RequestIdSupport.KEY, "req-276-gw");
    return exchange;
  }

  private void 라우트를_붙인다(
      MockServerWebExchange exchange, String 최종라우트, String 패턴을_남긴_라우트, String 패턴) {
    Route route =
        Route.async()
            .id(최종라우트)
            .uri(URI.create("http://core-service:8080"))
            .predicate(exchangeArg -> true)
            .build();

    exchange.getAttributes().put(GATEWAY_ROUTE_ATTR, route);
    exchange.getAttributes().put(GATEWAY_PREDICATE_MATCHED_PATH_ROUTE_ID_ATTR, 패턴을_남긴_라우트);
    exchange.getAttributes().put(GATEWAY_PREDICATE_MATCHED_PATH_ATTR, 패턴);
  }
}
