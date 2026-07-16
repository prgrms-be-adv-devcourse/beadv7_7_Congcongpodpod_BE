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

/**
 * 인증·인가 실패를 Gateway 공통 JSON 응답으로 변환한다.
 *
 * <p>{@link ServerAuthenticationEntryPoint}는 인증 자체가 성립하지 않은 401 상황을 처리하고, {@link
 * ServerAccessDeniedHandler}는 인증된 사용자의 권한이 부족한 403 상황을 처리한다.
 *
 * <p>토큰 만료, 서명 오류, issuer 불일치처럼 공격자가 이용할 수 있는 상세 검증 원인은 외부에 구분해서 노출하지 않는다. 클라이언트는 401이면 Access
 * Token 갱신을 시도하고, 403이면 현재 사용자의 권한 부족으로 처리할 수 있다.
 */
@NullMarked
@Component
public class GatewaySecurityErrorHandler
    implements ServerAuthenticationEntryPoint, ServerAccessDeniedHandler {

  private final ObjectMapper objectMapper;

  public GatewaySecurityErrorHandler(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  // 토큰 검증 실패의 구체적인 보안 원인은 클라이언트에 노출하지 않는다.
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
      // ErrorResponse를 직접 직렬화해 하위 서비스까지 요청을 보내지 않고 Gateway 응답을 종료한다.
      byte[] body = objectMapper.writeValueAsBytes(new ErrorResponse(code, message));
      return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
    } catch (JacksonException exception) {
      return Mono.error(exception);
    }
  }

  private record ErrorResponse(String code, String message) {}
}
