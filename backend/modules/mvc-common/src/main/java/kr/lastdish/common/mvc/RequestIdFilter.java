package kr.lastdish.common.mvc;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import kr.lastdish.common.api.tracing.RequestIdSupport;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Gateway가 전달한 {@code X-Request-Id}를 MDC에 올려, 이 요청을 처리하는 동안의 모든 로그가 같은 번호를 참조할 수 있게 한다.
 *
 * <p>서비스를 직접 호출해 헤더가 없거나 형식이 어긋난 경우에도 자체 발급해 로그가 번호 없이 남지 않게 한다. 스레드가 재사용되므로 요청이 끝나면 반드시 MDC를 정리한다.
 */
public class RequestIdFilter extends OncePerRequestFilter implements Ordered {

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String inbound = request.getHeader(RequestIdSupport.HEADER_NAME);
    String requestId = RequestIdSupport.isValid(inbound) ? inbound : UUID.randomUUID().toString();

    MDC.put(RequestIdSupport.KEY, requestId);

    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove(RequestIdSupport.KEY);
    }
  }
}
