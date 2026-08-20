package kr.lastdish.gateway.tracing;

import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

/**
 * Gateway의 요청 완료 로그를 남기는 공통 기록기.
 *
 * <p>정상 처리는 {@code RequestCompletionLoggingFilter}가, 라우팅·연결 실패는 {@code
 * GatewayGlobalExceptionHandler}가 호출한다. Gateway는 오류 응답을 WebFilter 바깥에서 만들기 때문에 두 경로가 필요하다. 대신 한 요청에
 * 두 줄이 남지 않도록 기록 여부를 exchange attribute로 직접 관리한다.
 *
 * <p>부하 테스트용 로그이므로 실제 경로의 식별자, query string, 헤더, 본문은 남기지 않는다.
 */
@Component
public class RequestCompletionLogger {

  private static final Logger log = LoggerFactory.getLogger(RequestCompletionLogger.class);

  private static final String STARTED_AT_ATTR =
      RequestCompletionLogger.class.getName() + ".startedAt";
  private static final String LOGGED_ATTR = RequestCompletionLogger.class.getName() + ".logged";

  /** 인프라가 주기적으로 호출하는 경로. 정상 응답이면 완료 로그를 남기지 않는다. */
  private static final String INFRASTRUCTURE_PATH_PREFIX = "/actuator";

  private final boolean skipSuccessfulActuatorCalls;

  /**
   * @param skipSuccessfulActuatorCalls 정상 {@code /actuator} 요청의 완료 로그를 생략할지. 이 로그에 기대는 대시보드나 알림이
   *     드러나면 재배포 없이 설정만 바꿔 되돌릴 수 있도록 열어둔다.
   */
  public RequestCompletionLogger(
      @Value("${request-log.skip-successful-actuator-calls:true}")
          boolean skipSuccessfulActuatorCalls) {
    this.skipSuccessfulActuatorCalls = skipSuccessfulActuatorCalls;
  }

  /** 요청 처리 시작 시각을 남긴다. 이 표시가 없으면 처리 시간을 알 수 없어 완료 로그를 남기지 않는다. */
  public void markStarted(ServerWebExchange exchange) {
    exchange.getAttributes().putIfAbsent(STARTED_AT_ATTR, System.nanoTime());
  }

  /** 완료 로그를 한 줄 남긴다. 이미 남긴 요청이면 아무것도 하지 않는다. */
  public void logCompletion(ServerWebExchange exchange) {
    Long startedAt = exchange.getAttribute(STARTED_AT_ATTR);
    HttpStatusCode status = exchange.getResponse().getStatusCode();

    // 시작 시각이나 최종 상태를 모르면 추측하지 않고 기록을 포기한다.
    if (startedAt == null || status == null) {
      return;
    }

    if (isRoutineInfrastructureCall(exchange, status)) {
      return;
    }

    // 먼저 도착한 호출만 통과시켜 한 요청에 한 줄만 남긴다.
    if (exchange.getAttributes().putIfAbsent(LOGGED_ATTR, Boolean.TRUE) != null) {
      return;
    }

    // 예외 핸들러에서 호출되면 Reactor 컨텍스트 밖이라 MDC가 비어 있다. 번호를 직접 올린 채 남긴다.
    RequestIdMdc.with(
        exchange,
        () ->
            log.info(
                "요청 처리가 완료되었습니다. method={}, pathPattern={}, status={}, durationMs={}",
                exchange.getRequest().getMethod(),
                RequestPathPatternResolver.resolve(exchange),
                status.value(),
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)));
  }

  /**
   * 인프라가 주기적으로 부르는 경로이면서 정상 응답이면 기록할 가치가 없다고 본다.
   *
   * <p>Gateway의 actuator 요청은 라우트에 매칭되지 않아 경로 패턴이 {@code unmatched}로만 남는다. 그래서 판단에는 실제 경로의 접두사를 쓰되,
   * 그 경로를 로그에 남기지는 않는다.
   */
  private boolean isRoutineInfrastructureCall(ServerWebExchange exchange, HttpStatusCode status) {
    return skipSuccessfulActuatorCalls
        && exchange.getRequest().getPath().value().startsWith(INFRASTRUCTURE_PATH_PREFIX)
        && !status.isError();
  }
}
