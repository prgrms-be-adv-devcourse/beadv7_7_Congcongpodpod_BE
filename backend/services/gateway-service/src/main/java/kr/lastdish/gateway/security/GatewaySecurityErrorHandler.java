package kr.lastdish.gateway.security;

import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@NullMarked
@Component
public class GatewaySecurityErrorHandler
    implements ServerAuthenticationEntryPoint, ServerAccessDeniedHandler {

  private final ObjectMapper objectMapper;

  public GatewaySecurityErrorHandler(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public Mono<Void> commence(
      ServerWebExchange exchange, AuthenticationException authenticationException) {
    return writeResponse(exchange, HttpStatus.UNAUTHORIZED, "INVALID_ACCESS_TOKEN", "인증이 필요합니다.");
  }

  @Override
  public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException exception) {
    return writeResponse(exchange, HttpStatus.FORBIDDEN, "ACCESS_DENIED", "접근 권한이 없습니다.");
  }

  private Mono<Void> writeResponse(
      ServerWebExchange exchange, HttpStatus status, String code, String message) {
    ServerHttpResponse response = exchange.getResponse();
    response.setStatusCode(status);
    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

    try {
      byte[] body = objectMapper.writeValueAsBytes(new ErrorResponse(code, message));
      return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
    } catch (JacksonException exception) {
      return Mono.error(exception);
    }
  }

  private record ErrorResponse(String code, String message) {}
}
