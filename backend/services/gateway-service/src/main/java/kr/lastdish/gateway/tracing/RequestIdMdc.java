package kr.lastdish.gateway.tracing;

import kr.lastdish.common.api.tracing.RequestIdSupport;
import org.slf4j.MDC;
import org.springframework.web.server.ServerWebExchange;

/**
 * exchange에 확정된 {@code requestId}를 MDC에 올린 채 로그를 남기게 한다.
 *
 * <p>{@link RequestIdFilter}가 Reactor 컨텍스트에 실은 값은 필터 체인 안에서만 MDC로 복원된다. 그런데 {@code
 * ErrorWebExceptionHandler}는 필터 체인보다 바깥에서 실행되므로 그 자리에서는 MDC가 비어 있다(테스트로 확인). 번호를 아는 쪽이 직접 올려두면 어느
 * 자리에서 남기든 로그에 {@code requestId} 필드가 붙는다.
 *
 * <p>이미 값이 올라와 있는 경우를 대비해 이전 값을 복원한다. 같은 스레드가 이어서 다른 일을 할 때 남은 값이 섞이지 않게 한다.
 */
public final class RequestIdMdc {

  private RequestIdMdc() {}

  /** exchange의 requestId를 MDC에 올린 채 작업을 실행하고, 끝나면 이전 상태로 되돌린다. */
  public static void with(ServerWebExchange exchange, Runnable action) {
    Object requestId = exchange.getAttribute(RequestIdSupport.KEY);
    // RequestIdFilter보다 앞선 단계에서 실패하면 번호가 없다. 필드를 비우지 않고 없었다는 사실을 남긴다.
    String resolved = requestId != null ? requestId.toString() : RequestIdSupport.UNKNOWN;

    String previous = MDC.get(RequestIdSupport.KEY);
    MDC.put(RequestIdSupport.KEY, resolved);

    try {
      action.run();
    } finally {
      if (previous != null) {
        MDC.put(RequestIdSupport.KEY, previous);
      } else {
        MDC.remove(RequestIdSupport.KEY);
      }
    }
  }
}
