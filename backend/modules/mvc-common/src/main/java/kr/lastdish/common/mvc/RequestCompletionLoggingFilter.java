package kr.lastdish.common.mvc;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import kr.lastdish.common.api.tracing.RequestIdSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

/**
 * 요청 처리가 끝났을 때 한 줄로 완료 사실을 남겨, 정상 요청과 느린 요청도 로그에서 찾을 수 있게 한다.
 *
 * <p>{@link RequestIdFilter}가 MDC를 정리하기 전에 실행되어야 하므로 그보다 한 단계 안쪽에 둔다. 반대로 {@code
 * DispatcherServlet}보다는 바깥이어야 {@code GlobalExceptionHandler}가 만든 최종 상태 코드를 읽을 수 있다.
 *
 * <p>이 레포에는 별도 logback 설정이 없어 로그 패턴에 {@code %X{requestId}}가 없다. 따라서 MDC에 의존하지 않고 메시지 안에 직접 {@code
 * requestId=}를 적는다.
 *
 * <p>부하 테스트 중 수집되는 로그이므로 경로의 실제 식별자, query string, 헤더, 본문은 남기지 않는다. 경로는 {@code HandlerMapping}이 매칭한
 * 패턴만 사용하고, 패턴을 구하지 못하면 실제 URI로 대체하지 않고 {@code unmatched}를 남긴다.
 */
public class RequestCompletionLoggingFilter extends OncePerRequestFilter implements Ordered {

  private static final Logger log = LoggerFactory.getLogger(RequestCompletionLoggingFilter.class);

  /** 매칭된 핸들러가 없어 경로 패턴을 알 수 없을 때 남기는 값. */
  private static final String UNMATCHED_PATH_PATTERN = "unmatched";

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 1;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    long startedAt = System.nanoTime();

    filterChain.doFilter(request, response);

    // 이 줄에 도달했다는 것은 응답이 정상적으로 만들어졌다는 뜻이다.
    // 예외가 빠져나가면 최종 상태를 알 수 없으므로 완료 로그를 남기지 않고 그대로 전파한다.
    log.info(
        "요청 처리가 완료되었습니다. requestId={}, method={}, pathPattern={}, status={}, durationMs={}",
        resolveRequestId(),
        request.getMethod(),
        resolvePathPattern(request),
        response.getStatus(),
        TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt));
  }

  /** RequestIdFilter가 MDC에 올린 번호를 읽는다. 확정 전 단계에서 실행됐다면 값이 없을 수 있다. */
  private String resolveRequestId() {
    String requestId = MDC.get(RequestIdSupport.KEY);
    return requestId != null ? requestId : RequestIdSupport.UNKNOWN;
  }

  /** DispatcherServlet이 남긴 매칭 패턴만 사용한다. 없으면 실제 URI 대신 unmatched를 남긴다. */
  private String resolvePathPattern(HttpServletRequest request) {
    Object pathPattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
    return pathPattern != null ? pathPattern.toString() : UNMATCHED_PATH_PATTERN;
  }
}
