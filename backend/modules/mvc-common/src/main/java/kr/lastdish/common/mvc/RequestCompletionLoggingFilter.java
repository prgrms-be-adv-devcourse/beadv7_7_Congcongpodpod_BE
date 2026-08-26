package kr.lastdish.common.mvc;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.OptionalInt;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

/**
 * 요청 처리가 끝났을 때 한 줄로 완료 사실을 남겨, 정상 요청과 느린 요청도 로그에서 찾을 수 있게 한다.
 *
 * <p>{@link RequestIdFilter}가 MDC를 정리하기 전에 실행되어야 하므로 그보다 한 단계 안쪽에 둔다. 반대로 {@code
 * DispatcherServlet}보다는 바깥이어야 {@code GlobalExceptionHandler}가 만든 최종 상태 코드를 읽을 수 있다.
 *
 * <p>{@code requestId}는 메시지에 적지 않는다. {@link RequestIdFilter}가 MDC에 올린 값이 구조화 로그의 필드로, 줄글 형식에서는
 * {@code logging.pattern.correlation} 자리로 붙는다.
 *
 * <p>상태 확인과 메트릭 수집처럼 인프라가 주기적으로 두드리는 요청은 정상일 때 남기지 않는다. 운영 실측에서 이 서비스의 완료 로그 1,263줄이 전부 {@code
 * /actuator} 요청이었고 실제 API 요청은 0건이었다. 다만 실패는 그 자체가 중요한 신호이므로 남긴다.
 *
 * <p>부하 테스트 중 수집되는 로그이므로 경로의 실제 식별자, query string, 헤더, 본문은 남기지 않는다. 경로는 {@code HandlerMapping}이 매칭한
 * 패턴만 사용하고, 패턴을 구하지 못하면 실제 URI로 대체하지 않고 {@code unmatched}를 남긴다.
 */
public class RequestCompletionLoggingFilter extends OncePerRequestFilter implements Ordered {

  private static final Logger log = LoggerFactory.getLogger(RequestCompletionLoggingFilter.class);

  /** 매칭된 핸들러가 없어 경로 패턴을 알 수 없을 때 남기는 값. */
  private static final String UNMATCHED_PATH_PATTERN = "unmatched";

  /** 인프라가 주기적으로 호출하는 경로. 정상 응답이면 완료 로그를 남기지 않는다. */
  private static final String INFRASTRUCTURE_PATH_PREFIX = "/actuator";

  /** 이 값 미만이면 정상 처리로 본다. */
  private static final int FIRST_FAILURE_STATUS = 400;

  private final boolean skipSuccessfulActuatorCalls;
  private final boolean countSqlStatements;

  /**
   * @param skipSuccessfulActuatorCalls 정상 {@code /actuator} 요청의 완료 로그를 생략할지. 이 로그에 기대는 대시보드나 알림이
   *     드러나면 재배포 없이 설정만 바꿔 되돌릴 수 있도록 열어둔다.
   * @param countSqlStatements 요청이 실행한 SQL 문의 수를 함께 남길지. 계측 오버헤드가 있으므로 기본은 끔이고, 측정할 때만 켠다.
   */
  public RequestCompletionLoggingFilter(
      @Value("${request-log.skip-successful-actuator-calls:true}")
          boolean skipSuccessfulActuatorCalls,
      @Value("${request-log.count-sql-statements:false}") boolean countSqlStatements) {
    this.skipSuccessfulActuatorCalls = skipSuccessfulActuatorCalls;
    this.countSqlStatements = countSqlStatements;
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 1;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    long startedAt = System.nanoTime();

    if (countSqlStatements) {
      SqlStatementCounter.start();
    }

    try {
      filterChain.doFilter(request, response);

      // 이 줄에 도달했다는 것은 응답이 정상적으로 만들어졌다는 뜻이다.
      // 예외가 빠져나가면 최종 상태를 알 수 없으므로 완료 로그를 남기지 않고 그대로 전파한다.
      String pathPattern = resolvePathPattern(request);

      if (isRoutineInfrastructureCall(pathPattern, response.getStatus())) {
        return;
      }

      // requestId는 RequestIdFilter가 올린 MDC를 통해 로그 필드로 붙으므로 메시지에 넣지 않는다.
      log.info(
          "요청 처리가 완료되었습니다. method={}, pathPattern={}, status={}, durationMs={}{}",
          request.getMethod(),
          pathPattern,
          response.getStatus(),
          TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt),
          queryCountSuffix());
    } finally {
      // 스레드 풀이 이 스레드를 다음 요청에 재사용하므로, 어떤 경로로 빠져나가든 반드시 비운다.
      SqlStatementCounter.clear();
    }
  }

  /** 계측 중일 때만 실행한 SQL 수를 메시지 끝에 덧붙인다. 꺼져 있으면 기존 로그 형식 그대로 남는다. */
  private String queryCountSuffix() {
    OptionalInt count = SqlStatementCounter.count();
    return count.isPresent() ? ", queryCount=" + count.getAsInt() : "";
  }

  /** 인프라가 주기적으로 부르는 경로이면서 정상 응답이면 기록할 가치가 없다고 본다. */
  private boolean isRoutineInfrastructureCall(String pathPattern, int status) {
    return skipSuccessfulActuatorCalls
        && pathPattern.startsWith(INFRASTRUCTURE_PATH_PREFIX)
        && status < FIRST_FAILURE_STATUS;
  }

  /** DispatcherServlet이 남긴 매칭 패턴만 사용한다. 없으면 실제 URI 대신 unmatched를 남긴다. */
  private String resolvePathPattern(HttpServletRequest request) {
    Object pathPattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
    return pathPattern != null ? pathPattern.toString() : UNMATCHED_PATH_PATTERN;
  }
}
