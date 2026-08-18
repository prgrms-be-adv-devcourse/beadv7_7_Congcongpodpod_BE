package kr.lastdish.gateway.tracing;

import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * 정상적으로 끝난 요청의 완료 로그를 남긴다.
 *
 * <p>{@link RequestIdFilter}가 확정한 번호를 읽어야 하므로 그보다 한 단계 안쪽에 둔다.
 *
 * <p>라우팅·연결 실패는 이 필터를 뚫고 나가 {@code GatewayGlobalExceptionHandler}가 처리하므로 여기서는 잡히지 않는다. 그쪽 경로의 완료
 * 로그는 예외 처리기가 오류 응답을 실제로 쓴 뒤 같은 기록기를 호출해 남긴다.
 *
 * <p>{@code doOnSuccess}는 정상 완료에만 실행된다. 오류와 취소는 통과하지 않으므로, 응답을 끝내지 못한 요청이 성공으로 기록되지 않는다.
 */
@Component
public class RequestCompletionLoggingFilter implements WebFilter, Ordered {

  private final RequestCompletionLogger completionLogger;

  public RequestCompletionLoggingFilter(RequestCompletionLogger completionLogger) {
    this.completionLogger = completionLogger;
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 1;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    completionLogger.markStarted(exchange);

    return chain.filter(exchange).doOnSuccess(unused -> completionLogger.logCompletion(exchange));
  }
}
