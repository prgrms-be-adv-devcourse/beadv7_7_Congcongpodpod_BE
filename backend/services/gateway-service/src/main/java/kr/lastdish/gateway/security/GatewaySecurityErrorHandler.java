package kr.lastdish.gateway.security;

import kr.lastdish.common.api.response.ApiResponse;
import kr.lastdish.gateway.error.GatewayErrorCode;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.cloud.gateway.config.GlobalCorsProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
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
  private final UrlBasedCorsConfigurationSource corsConfigurationSource;

  /**
   * {@code globalcors}(application.yml)에 이미 설정된 origin 규칙을 직접 재사용한다. {@code
   * CorsConfigurationSource} 타입 Bean으로 따로 등록하지 않고 이 클래스 전용 필드로만 들고 있는 이유는, Spring Boot가 그 타입의 Bean
   * 존재만으로 WebFlux CORS preflight(OPTIONS) 처리에 자동 개입해 기존 {@code pathMatchers(OPTIONS,
   * "/api/**").permitAll()} 경로까지 403으로 막는 걸 확인했기 때문이다(2026-07-30, {@code
   * GatewaySecurityConfigTests} 회귀로 발견). 여기서는 401/403 응답에 헤더 하나 붙이는 용도로만 조회한다.
   */
  public GatewaySecurityErrorHandler(
      ObjectMapper objectMapper, GlobalCorsProperties globalCorsProperties) {
    this.objectMapper = objectMapper;
    this.corsConfigurationSource = new UrlBasedCorsConfigurationSource();
    this.corsConfigurationSource.setCorsConfigurations(
        globalCorsProperties.getCorsConfigurations());
  }

  /**
   * 인증 실패 시 Spring Security가 호출하는 401 진입점이다.
   *
   * <p>Entry Point는 인증을 시작하는 컨트롤러라는 뜻이 아니라, 보호 자원에 접근할 인증이 성립하지 않았을 때 응답을 시작하는 지점이다. 토큰 누락·만료·서명
   * 오류의 상세 원인은 클라이언트에 노출하지 않는다.
   */
  @Override
  public Mono<Void> commence(
      ServerWebExchange exchange, AuthenticationException authenticationException) {
    return writeResponse(exchange, GatewayErrorCode.INVALID_ACCESS_TOKEN);
  }

  /**
   * 인증된 사용자의 인가 실패 시 Spring Security가 호출하는 403 처리기다.
   *
   * <p>예를 들어 ROLE_MEMBER 사용자가 SELLER 전용 경로에 접근하면 토큰 자체는 유효하므로 재로그인 대상인 401이 아니라 권한 부족을 나타내는 403을
   * 반환한다.
   */
  @Override
  public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException exception) {
    return writeResponse(exchange, GatewayErrorCode.ACCESS_DENIED);
  }

  private Mono<Void> writeResponse(ServerWebExchange exchange, GatewayErrorCode errorCode) {
    ServerHttpResponse response = exchange.getResponse();
    response.setStatusCode(errorCode.getStatus());
    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
    addCorsHeadersIfAllowed(exchange);

    try {
      // 공통 ApiResponse를 직접 직렬화해 하위 서비스까지 요청을 보내지 않고 Gateway 응답을 종료한다.
      byte[] body =
          objectMapper.writeValueAsBytes(
              ApiResponse.fail(errorCode.getCode(), errorCode.getMessage()));
      return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
    } catch (JacksonException exception) {
      return Mono.error(exception);
    }
  }

  /**
   * globalcors와 같은 설정을 참조해, 이 요청의 Origin이 허용 목록에 있으면 {@code Access-Control-Allow-Origin}을 붙인다.
   *
   * <p>정상 라우팅 응답은 Gateway가 globalcors로 헤더를 붙이지만, 여기서 끝나는 401/403 응답은 그 필터를 거치지 않는다 — 붙이지 않으면 브라우저가
   * 실제 401/403 바디 대신 CORS 에러로 표시해 원인 파악이 어려워진다.
   */
  private void addCorsHeadersIfAllowed(ServerWebExchange exchange) {
    ServerHttpRequest request = exchange.getRequest();
    String origin = request.getHeaders().getOrigin();
    if (origin == null) {
      return;
    }

    CorsConfiguration configuration = corsConfigurationSource.getCorsConfiguration(exchange);
    if (configuration == null) {
      return;
    }

    @Nullable String allowedOrigin = configuration.checkOrigin(origin);
    if (allowedOrigin == null) {
      return;
    }

    HttpHeaders headers = exchange.getResponse().getHeaders();
    headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, allowedOrigin);
    headers.add(HttpHeaders.VARY, HttpHeaders.ORIGIN);
  }
}
