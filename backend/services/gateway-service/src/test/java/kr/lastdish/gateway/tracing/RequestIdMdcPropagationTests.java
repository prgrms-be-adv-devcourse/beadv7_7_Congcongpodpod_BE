package kr.lastdish.gateway.tracing;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.context.ContextRegistry;
import java.util.concurrent.atomic.AtomicReference;
import kr.lastdish.common.api.tracing.RequestIdSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;

/** 운영에서는 spring.reactor.context-propagation=auto가 켜주는 동작을 테스트에서 직접 켜서 검증한다. */
class RequestIdMdcPropagationTests {

  private final RequestIdFilter filter = new RequestIdFilter();

  @BeforeEach
  void 자동_전파를_켠다() {
    Hooks.enableAutomaticContextPropagation();
  }

  @AfterEach
  void 원래대로_되돌린다() {
    Hooks.disableAutomaticContextPropagation();
    MDC.clear();
  }

  @Test
  @DisplayName("하위 체인이 MDC에서 requestId를 읽을 수 있다")
  void 하위_체인에서_MDC로_읽을_수_있다() {
    AtomicReference<String> seen = new AtomicReference<>();
    WebFilterChain chain =
        exchange -> Mono.fromRunnable(() -> seen.set(MDC.get(RequestIdSupport.KEY)));

    filter.filter(exchangeWithRequestId("mdc-test-1"), chain).block();

    assertThat(seen.get()).isEqualTo("mdc-test-1");
  }

  @Test
  @DisplayName("요청 처리가 끝나면 MDC에 값이 남지 않는다")
  void 요청이_끝나면_MDC가_비어_있다() {
    WebFilterChain chain = exchange -> Mono.empty();

    filter.filter(exchangeWithRequestId("mdc-test-2"), chain).block();

    assertThat(MDC.get(RequestIdSupport.KEY)).isNull();
  }

  @Test
  @DisplayName("접근자가 ServiceLoader로 자동 등록된다")
  void 접근자가_자동으로_등록된다() {
    boolean registered =
        ContextRegistry.getInstance().getThreadLocalAccessors().stream()
            .anyMatch(accessor -> accessor instanceof RequestIdThreadLocalAccessor);

    assertThat(registered).as("META-INF/services 등록이 빠지면 운영에서 조용히 동작하지 않는다").isTrue();
  }

  private MockServerWebExchange exchangeWithRequestId(String requestId) {
    return MockServerWebExchange.from(
        MockServerHttpRequest.get("/api/v1/stores/1")
            .header(RequestIdSupport.HEADER_NAME, requestId));
  }
}
