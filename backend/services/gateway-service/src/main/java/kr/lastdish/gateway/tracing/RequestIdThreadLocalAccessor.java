package kr.lastdish.gateway.tracing;

import io.micrometer.context.ThreadLocalAccessor;
import kr.lastdish.common.api.tracing.RequestIdSupport;
import org.slf4j.MDC;

/**
 * Reactor 컨텍스트에 담긴 {@code requestId}를 연산자가 실행되는 스레드의 MDC로 옮긴다.
 *
 * <p>WebFlux는 한 요청을 여러 스레드가 나눠 처리하므로 MDC에 직접 넣어두면 값이 따라가지 않는다. 그래서 Gateway는 값을 Reactor 컨텍스트에 싣고,
 * Reactor가 연산자를 실행하기 직전에 이 접근자를 통해 MDC를 채운 뒤 실행이 끝나면 되돌린다. 구조화 로깅은 MDC만 읽으므로 이 다리가 있어야 Gateway 로그에도
 * {@code requestId} 필드가 남는다.
 *
 * <p>동작하려면 {@code spring.reactor.context-propagation=auto}가 함께 켜져 있어야 한다.
 */
public class RequestIdThreadLocalAccessor implements ThreadLocalAccessor<String> {

  @Override
  public Object key() {
    return RequestIdSupport.KEY;
  }

  @Override
  public String getValue() {
    return MDC.get(RequestIdSupport.KEY);
  }

  @Override
  public void setValue(String value) {
    MDC.put(RequestIdSupport.KEY, value);
  }

  @Override
  public void setValue() {
    MDC.remove(RequestIdSupport.KEY);
  }
}
