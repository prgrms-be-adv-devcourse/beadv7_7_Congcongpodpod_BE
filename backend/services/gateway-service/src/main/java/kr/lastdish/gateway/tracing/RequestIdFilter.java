package kr.lastdish.gateway.tracing;

import java.util.UUID;
import kr.lastdish.common.api.tracing.RequestIdSupport;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * 요청마다 {@code requestId}를 확정해 하위 서비스로 전파하고, 응답 헤더와 exchange attribute에 남긴다.
 *
 * <p>인바운드 {@code X-Request-Id}는 외부(현재는 Ingress 경유)가 통제하는 값이므로 형식을 검증한 뒤에만 이어받는다. 검증에 실패하거나 값이 없으면
 * 새로 발급한다. 이 필터는 요청 처리 초반에 실행되어야 하므로 최우선 순위로 둔다.
 *
 * <p>{@link WebFilter}로 구현한다. {@code GlobalFilter}는 {@code FilteringWebHandler}가 실행하는데, 이는 Spring
 * Security의 {@code SecurityWebFilterChain}(WebFilter)과 핸들러 매핑보다 나중이라 인증 실패(401/403)·라우트 미매칭(404)
 * 응답과 그 경로에서 발생하는 예외에는 requestId가 붙지 않는다. WebFilter로 구현하면 이 필터가 가장 먼저 실행되어 모든 경로를 포함한다.
 */
@Component
public class RequestIdFilter implements WebFilter, Ordered {

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    String requestId = resolveRequestId(exchange.getRequest());

    ServerHttpRequest request =
        exchange
            .getRequest()
            .mutate()
            .headers(headers -> headers.set(RequestIdSupport.HEADER_NAME, requestId))
            .build();

    ServerWebExchange mutatedExchange = exchange.mutate().request(request).build();
    mutatedExchange.getAttributes().put(RequestIdSupport.KEY, requestId);
    mutatedExchange.getResponse().getHeaders().set(RequestIdSupport.HEADER_NAME, requestId);

    return chain.filter(mutatedExchange);
  }

  private String resolveRequestId(ServerHttpRequest request) {
    String inbound = request.getHeaders().getFirst(RequestIdSupport.HEADER_NAME);

    if (RequestIdSupport.isValid(inbound)) {
      return inbound;
    }

    return UUID.randomUUID().toString();
  }
}
