package kr.lastdish.gateway.error;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.TimeoutException;
import kr.lastdish.common.api.exception.ErrorCodeSpec;
import kr.lastdish.common.api.response.ApiResponse;
import kr.lastdish.common.api.tracing.RequestIdSupport;
import kr.lastdish.gateway.tracing.RequestCompletionLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Gateway의 라우팅 및 하위 서비스 연결 과정에서 발생한 예외를 공통 API 오류 응답으로 변환한다.
 *
 * <p>Gateway는 WebFlux 기반이므로 MVC의 {@code GlobalExceptionHandler} 대신 {@link
 * ErrorWebExceptionHandler}를 사용한다.
 */
@Component
// Spring Boot의 기본 WebFlux 오류 처리기보다 먼저 실행한다.
@Order(-2)
public class GatewayGlobalExceptionHandler implements ErrorWebExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GatewayGlobalExceptionHandler.class);

  /** 하위 서비스 호출 실패로 판별하는 예외 종류와, 그때 사용할 오류 코드의 대응. */
  private record DependencyFailure(Class<? extends Throwable> causeType, ErrorCodeSpec errorCode) {}

  // 오류 코드 판별과 로그 원인 조회가 같은 순서를 보도록 목록 하나만 둔다.
  private static final List<DependencyFailure> DEPENDENCY_FAILURES =
      List.of(
          new DependencyFailure(TimeoutException.class, GatewayErrorCode.GATEWAY_TIMEOUT),
          new DependencyFailure(ConnectException.class, GatewayErrorCode.SERVICE_UNAVAILABLE),
          new DependencyFailure(NoRouteToHostException.class, GatewayErrorCode.SERVICE_UNAVAILABLE),
          new DependencyFailure(UnknownHostException.class, GatewayErrorCode.SERVICE_UNAVAILABLE));

  private final ObjectMapper objectMapper;
  private final RequestCompletionLogger completionLogger;

  public GatewayGlobalExceptionHandler(
      ObjectMapper objectMapper, RequestCompletionLogger completionLogger) {
    this.objectMapper = objectMapper;
    this.completionLogger = completionLogger;
  }

  @Override
  public Mono<Void> handle(ServerWebExchange exchange, Throwable exception) {
    // 이미 응답 전송이 시작되었다면 새로운 오류 본문을 쓸 수 없으므로 상위 처리 흐름으로 전달한다.
    if (exchange.getResponse().isCommitted()) {
      return Mono.error(exception);
    }

    ErrorCodeSpec errorCode = resolveErrorCode(exception);

    logFailure(exchange, errorCode, exception);

    // 오류 응답은 WebFilter 바깥에서 만들어지므로 RequestCompletionLoggingFilter가 이 경로를 보지 못한다.
    // 응답 쓰기가 정상적으로 끝난 뒤 같은 기록기를 호출해 완료 로그를 남긴다. 중복 방지는 기록기가 맡는다.
    return writeResponse(exchange, errorCode)
        .doOnSuccess(unused -> completionLogger.logCompletion(exchange));
  }

  /**
   * Gateway가 직접 관측한 사실만 기록한다.
   *
   * <p>하위 서비스 내부에서 발생한 예외의 원인과 스택은 해당 서비스가 기록하므로 여기서 중복해서 남기지 않는다. 일반적인 4xx는 로그를 남기지 않고 이후 공통 요청 완료
   * 로그와 메트릭으로 집계한다.
   */
  private void logFailure(
      ServerWebExchange exchange, ErrorCodeSpec errorCode, Throwable exception) {
    String requestId = resolveRequestId(exchange);

    if (errorCode == GatewayErrorCode.INTERNAL_ERROR) {
      log.error(
          "Gateway 요청 처리 중 오류가 발생했습니다. requestId={}, method={}, path={}, errorCode={}, exceptionClass={}",
          requestId,
          exchange.getRequest().getMethod(),
          exchange.getRequest().getPath(),
          errorCode.getCode(),
          exception.getClass().getName(),
          exception);

      return;
    }

    if (isDependencyFailure(errorCode)) {
      // 부하 상황에서 반복될 수 있으므로 스택 없이 남기고, 원인 판별에 사용한 예외 종류만 함께 기록한다.
      log.warn(
          "하위 서비스 호출에 실패했습니다. requestId={}, method={}, path={}, errorCode={}, exceptionClass={}",
          requestId,
          exchange.getRequest().getMethod(),
          exchange.getRequest().getPath(),
          errorCode.getCode(),
          resolveFailureCauseName(exception));
    }
  }

  private boolean isDependencyFailure(ErrorCodeSpec errorCode) {
    return errorCode == GatewayErrorCode.GATEWAY_TIMEOUT
        || errorCode == GatewayErrorCode.SERVICE_UNAVAILABLE
        || errorCode == GatewayErrorCode.BAD_GATEWAY;
  }

  /** RequestIdFilter보다 앞선 단계에서 예외가 나면 requestId가 없을 수 있다. 그 경우 값이 없다는 사실 자체를 로그에 남긴다. */
  private String resolveRequestId(ServerWebExchange exchange) {
    Object requestId = exchange.getAttribute(RequestIdSupport.KEY);
    return requestId != null ? requestId.toString() : RequestIdSupport.UNKNOWN;
  }

  /** 오류 코드를 결정한 실제 원인 예외의 클래스명을 찾는다. 원인을 특정하지 못하면 전달받은 예외의 클래스명을 사용한다. */
  private String resolveFailureCauseName(Throwable exception) {
    for (DependencyFailure dependencyFailure : DEPENDENCY_FAILURES) {
      Throwable cause = findCause(exception, dependencyFailure.causeType());

      if (cause != null) {
        return cause.getClass().getName();
      }
    }

    return exception.getClass().getName();
  }

  private ErrorCodeSpec resolveErrorCode(Throwable exception) {
    if (exception instanceof ResponseStatusException statusException) {
      return resolveStatus(statusException.getStatusCode());
    }

    // 실행 환경에 따라 동일한 하위 서비스 연결 실패가 서로 다른 네트워크 예외로 감싸질 수 있다.
    for (DependencyFailure dependencyFailure : DEPENDENCY_FAILURES) {
      if (findCause(exception, dependencyFailure.causeType()) != null) {
        return dependencyFailure.errorCode();
      }
    }

    return GatewayErrorCode.INTERNAL_ERROR;
  }

  private ErrorCodeSpec resolveStatus(HttpStatusCode status) {
    return switch (status.value()) {
      case 400 -> GatewayErrorCode.INVALID_REQUEST;
      case 404 -> GatewayErrorCode.ROUTE_NOT_FOUND;
      case 502 -> GatewayErrorCode.BAD_GATEWAY;
      case 503 -> GatewayErrorCode.SERVICE_UNAVAILABLE;
      case 504 -> GatewayErrorCode.GATEWAY_TIMEOUT;
      default -> GatewayErrorCode.INTERNAL_ERROR;
    };
  }

  private Throwable findCause(Throwable exception, Class<? extends Throwable> causeType) {
    Throwable current = exception;

    // Reactor/Netty가 실제 네트워크 예외를 여러 단계로 감싸므로 전체 원인 체인을 확인한다.
    while (current != null) {
      if (causeType.isInstance(current)) {
        return current;
      }

      current = current.getCause();
    }

    return null;
  }

  private Mono<Void> writeResponse(ServerWebExchange exchange, ErrorCodeSpec errorCode) {
    ServerHttpResponse response = exchange.getResponse();
    response.setStatusCode(errorCode.getStatus());
    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

    try {
      byte[] body =
          objectMapper.writeValueAsBytes(
              ApiResponse.fail(errorCode.getCode(), errorCode.getMessage()));

      return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
    } catch (JacksonException serializationException) {
      return Mono.error(serializationException);
    }
  }
}
