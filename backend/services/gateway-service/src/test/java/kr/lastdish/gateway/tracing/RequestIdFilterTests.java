package kr.lastdish.gateway.tracing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.regex.Pattern;
import kr.lastdish.common.api.tracing.RequestIdSupport;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class RequestIdFilterTests {

  private static final Pattern UUID_FORMAT =
      Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");

  private final RequestIdFilter filter = new RequestIdFilter();

  @Test
  void 인바운드_헤더가_없으면_새로_발급한다() {
    CapturingFilterChain chain = filterWithRequest(MockServerHttpRequest.get("/api/v1/stores/1"));

    String requestId = downstreamRequestId(chain);

    assertThat(requestId).matches(UUID_FORMAT);
  }

  @Test
  void 정상형식의_인바운드_값은_그대로_이어받는다() {
    CapturingFilterChain chain =
        filterWithRequest(
            MockServerHttpRequest.get("/api/v1/stores/1")
                .header(RequestIdSupport.HEADER_NAME, "edge-abc-123"));

    assertThat(downstreamRequestId(chain)).isEqualTo("edge-abc-123");
  }

  @Test
  void 허용하지_않는_문자가_섞인_값은_버리고_새로_발급한다() {
    CapturingFilterChain chain =
        filterWithRequest(
            MockServerHttpRequest.get("/api/v1/stores/1")
                .header(RequestIdSupport.HEADER_NAME, "abc\ndef"));

    assertThat(downstreamRequestId(chain)).matches(UUID_FORMAT);
  }

  @Test
  void 길이상한을_넘는_값은_버리고_새로_발급한다() {
    CapturingFilterChain chain =
        filterWithRequest(
            MockServerHttpRequest.get("/api/v1/stores/1")
                .header(RequestIdSupport.HEADER_NAME, "a".repeat(65)));

    assertThat(downstreamRequestId(chain)).matches(UUID_FORMAT);
  }

  @Test
  void 확정된_번호를_응답헤더와_attribute에도_남긴다() {
    CapturingFilterChain chain =
        filterWithRequest(
            MockServerHttpRequest.get("/api/v1/stores/1")
                .header(RequestIdSupport.HEADER_NAME, "edge-abc-123"));

    ServerWebExchange exchange = chain.exchange();

    assertThat(exchange.getResponse().getHeaders().getFirst(RequestIdSupport.HEADER_NAME))
        .isEqualTo("edge-abc-123");
    assertThat((String) exchange.getAttribute(RequestIdSupport.KEY)).isEqualTo("edge-abc-123");
  }

  private CapturingFilterChain filterWithRequest(MockServerHttpRequest.BaseBuilder<?> builder) {
    ServerWebExchange exchange = MockServerWebExchange.from(builder.build());
    CapturingFilterChain chain = new CapturingFilterChain();

    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

    return chain;
  }

  private String downstreamRequestId(CapturingFilterChain chain) {
    HttpHeaders headers = chain.exchange().getRequest().getHeaders();
    return Objects.requireNonNull(headers.getFirst(RequestIdSupport.HEADER_NAME));
  }

  private static final class CapturingFilterChain implements GatewayFilterChain {

    private @Nullable ServerWebExchange exchange;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange) {
      this.exchange = exchange;
      return Mono.empty();
    }

    ServerWebExchange exchange() {
      return Objects.requireNonNull(exchange);
    }
  }
}
