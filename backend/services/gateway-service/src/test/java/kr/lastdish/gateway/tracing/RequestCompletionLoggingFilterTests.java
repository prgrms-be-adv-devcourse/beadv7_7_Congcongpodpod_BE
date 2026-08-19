package kr.lastdish.gateway.tracing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import kr.lastdish.common.api.tracing.RequestIdSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

class RequestCompletionLoggingFilterTests {

  private final RequestCompletionLoggingFilter filter =
      new RequestCompletionLoggingFilter(new RequestCompletionLogger());

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
  void 정상적으로_끝나면_완료_로그를_한_줄_남긴다() {
    MockServerWebExchange exchange = 요청();
    exchange.getResponse().setStatusCode(HttpStatus.OK);
    WebFilterChain 정상체인 = e -> Mono.empty();

    filter.filter(exchange, 정상체인).block();

    assertThat(appender.list).hasSize(1);
    assertThat(appender.list.getFirst().getFormattedMessage())
        .contains("requestId=req-276-gw")
        .contains("status=200");
  }

  @Test
  void 체인에서_오류가_나면_완료_로그를_남기지_않는다() {
    MockServerWebExchange exchange = 요청();
    WebFilterChain 오류체인 = e -> Mono.error(new RuntimeException("ConnectException"));

    assertThatThrownBy(() -> filter.filter(exchange, 오류체인).block())
        .isInstanceOf(RuntimeException.class);

    assertThat(appender.list).isEmpty();
  }

  @Test
  void 취소된_요청은_완료_로그를_남기지_않는다() {
    MockServerWebExchange exchange = 요청();
    exchange.getResponse().setStatusCode(HttpStatus.OK);
    WebFilterChain 끝나지_않는_체인 = e -> Mono.never();

    Disposable 구독 = filter.filter(exchange, 끝나지_않는_체인).subscribe();
    구독.dispose();

    assertThat(appender.list).isEmpty();
  }

  @Test
  void RequestIdFilter보다_안쪽에서_실행되도록_순서값이_더_크다() {
    assertThat(filter.getOrder()).isGreaterThan(new RequestIdFilter().getOrder());
  }

  private MockServerWebExchange 요청() {
    MockServerWebExchange exchange =
        MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/orders/8817342").build());
    exchange.getAttributes().put(RequestIdSupport.KEY, "req-276-gw");
    return exchange;
  }
}
