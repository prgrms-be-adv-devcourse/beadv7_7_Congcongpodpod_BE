package kr.lastdish.gateway.error;

import java.net.ConnectException;
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

@Component
@Order(-2)
public class GatewayGlobalExceptionHandler implements ErrorWebExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GatewayGlobalExceptionHandler.class);

  private final ObjectMapper objectMapper;

  public GatewayGlobalExceptionHandler(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public Mono<Void> handle(ServerWebExchange exchange, Throwable exception) {
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

    if (hasCause(exception, ConnectException.class)) {
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
      default -> GatewayErrorCode.INTERNAL_ERROR;
    };
  }

  private boolean hasCause(Throwable exception, Class<? extends Throwable> causeType) {
    Throwable current = exception;

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
