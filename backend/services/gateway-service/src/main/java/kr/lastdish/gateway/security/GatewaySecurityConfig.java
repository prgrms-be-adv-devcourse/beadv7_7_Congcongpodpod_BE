package kr.lastdish.gateway.security;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Gateway 라우팅 전에 실행되는 WebFlux Security 필터 체인을 구성한다.
 *
 * <p>보호 API는 Bearer Access Token을 Gateway에서 자체 검증하며 일반 요청마다 Member Service를 호출하지 않는다. 인증에 성공해야 요청이
 * Gateway 라우팅 단계로 진행되고, 실패하면 여기서 401/403 응답이 종료된다.
 *
 * <p>경로 규칙은 위에서 아래로 처음 일치한 규칙이 적용된다. 따라서 좁은 공개 경로와 SELLER 전용 경로를 먼저 선언하고, 마지막에는 명시되지 않은 경로를 모두
 * 거부한다.
 */
@Configuration
public class GatewaySecurityConfig {

  /**
   * Spring이 애플리케이션 시작 시 호출해 보안 WebFilter 목록을 Bean으로 등록한다.
   *
   * <p>개발 코드가 이 메서드를 직접 호출하지 않는다. 이후 HTTP 요청이 들어오면 Spring WebFlux가 완성된 {@link
   * SecurityWebFilterChain}을 Gateway 라우팅보다 먼저 실행한다.
   */
  @Bean
  SecurityWebFilterChain gatewaySecurityFilterChain(
      ServerHttpSecurity http,
      JwtRoleConverter jwtRoleConverter,
      GatewaySecurityErrorHandler securityErrorHandler) {
    return http
        // csrf: 쿠키 기반 세션 인증을 보호하는 CSRF 토큰 검사를 비활성화한다. 이 API는 Bearer Token을 사용한다.
        .csrf(ServerHttpSecurity.CsrfSpec::disable)
        // httpBasic: Authorization: Basic 인증과 브라우저의 Basic 인증 팝업을 사용하지 않는다.
        .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
        // formLogin: 로그인 HTML과 서버 세션 기반 로그인을 사용하지 않는다. 로그인은 Member API가 담당한다.
        .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
        // authorizeExchange: HTTP 메서드와 경로별로 필요한 인증/역할을 선언한다.
        .authorizeExchange(
            exchange ->
                exchange
                    // 회원가입, 로그인, Refresh Token 재발급은 Access Token 없이 요청한다.
                    .pathMatchers(
                        POST, "/api/v1/auth/signup", "/api/v1/auth/login", "/api/v1/auth/refresh")
                    .permitAll()
                    // Kubernetes readiness/liveness probe는 인증 없이 접근해야 한다.
                    .pathMatchers("/actuator/health/**")
                    .permitAll()
                    // 로컬 API 문서와 UI는 인증 전에 조회되어야 한다. 운영에서는 springdoc 자체가 비활성화된다.
                    .pathMatchers(
                        "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/openapi/**")
                    .permitAll()
                    // 가게와 상품의 공개 조회는 인증 없이 허용한다.
                    .pathMatchers(GET, "/api/v1/stores/**", "/api/v1/dishes/**")
                    .permitAll()
                    // 가게·상품 변경과 정산·입금 기능은 판매자만 이용한다.
                    .pathMatchers(
                        "/api/v1/stores/**",
                        "/api/v1/dishes/**",
                        "/api/v1/settlements/**",
                        "/api/v1/deposits/**")
                    .hasRole("SELLER")
                    // 로그아웃과 회원·장바구니·주문·결제 기능은 로그인한 회원이 이용한다.
                    .pathMatchers(
                        "/api/v1/auth/logout",
                        "/api/v1/members/**",
                        "/api/v1/carts/**",
                        "/api/v1/orders/**",
                        "/api/v1/payments/**")
                    .hasAnyRole("MEMBER", "SELLER")
                    // 실수로 새 API가 무인증 공개되는 것을 막는 기본 거부 정책이다.
                    .anyExchange()
                    .denyAll())
        // exceptionHandling: authorizeExchange 등 전체 보안 체인에서 발생한 실패 처리기를 등록한다.
        .exceptionHandling(
            exceptions ->
                exceptions
                    // 인증 정보가 없거나 인증이 성립하지 않으면 commence()를 호출해 401을 작성한다.
                    .authenticationEntryPoint(securityErrorHandler)
                    // 인증은 됐지만 위의 hasRole 규칙을 만족하지 않으면 handle()을 호출해 403을 작성한다.
                    .accessDeniedHandler(securityErrorHandler))
        // oauth2ResourceServer: Authorization: Bearer 값을 읽어 이 Gateway를 JWT Resource Server로 동작시킨다.
        .oauth2ResourceServer(
            oauth2 ->
                oauth2
                    // Bearer Token 누락/형식 오류/서명·만료 검증 실패처럼 Resource Server 내부의 인증 실패를 처리한다.
                    .authenticationEntryPoint(securityErrorHandler)
                    // Resource Server 내부에서 인증 후 권한이 거부되는 경우를 처리한다.
                    .accessDeniedHandler(securityErrorHandler)
                    // jwt: ReactiveJwtDecoder로 검증한 Jwt를 Spring Authentication으로 변환한다.
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtRoleConverter)))
        // 위 설정을 실제 요청에 적용할 불변 SecurityWebFilterChain으로 완성한다.
        .build();
  }
}
