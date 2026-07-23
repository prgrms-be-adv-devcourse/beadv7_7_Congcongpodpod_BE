package kr.lastdish.gateway.error;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;
import kr.lastdish.common.api.exception.ErrorCodeSpec;
import kr.lastdish.common.api.response.ApiResponse;
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

  private final ObjectMapper objectMapper;

  public GatewayGlobalExceptionHandler(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public Mono<Void> handle(ServerWebExchange exchange, Throwable exception) {
    // 이미 응답 전송이 시작되었다면 새로운 오류 본문을 쓸 수 없으므로 상위 처리 흐름으로 전달한다.
    if (exchange.getResponse().isCommitted()) {
      return Mono.error(exception);
    }

    ErrorCodeSpec errorCode = resolveErrorCode(exception);

    if (errorCode == GatewayErrorCode.INTERNAL_ERROR) {
      log.error(
          "Gateway 요청 처리 중 오류가 발생했습니다. method={}, path={}",
          exchange.getRequest().getMethod(),
          exchange.getRequest().getPath(),
          exception);
    }

    return writeResponse(exchange, errorCode);
  }

  private ErrorCodeSpec resolveErrorCode(Throwable exception) {
    if (exception instanceof ResponseStatusException statusException) {
      return resolveStatus(statusException.getStatusCode());
    }

    if (hasCause(exception, TimeoutException.class)) {
      return GatewayErrorCode.GATEWAY_TIMEOUT;
    }

    // 실행 환경에 따라 동일한 하위 서비스 연결 실패가 서로 다른 네트워크 예외로 감싸질 수 있다.
    if (hasCause(exception, ConnectException.class)
        || hasCause(exception, NoRouteToHostException.class)
        || hasCause(exception, UnknownHostException.class)) {
      return GatewayErrorCode.SERVICE_UNAVAILABLE;
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

  private boolean hasCause(Throwable exception, Class<? extends Throwable> causeType) {
    Throwable current = exception;

    // Reactor/Netty가 실제 네트워크 예외를 여러 단계로 감싸므로 전체 원인 체인을 확인한다.
    while (current != null) {
      if (causeType.isInstance(current)) {
        return true;
      }

      current = current.getCause();
    }

    return false;
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
