package kr.lastdish.gateway.security;

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

  @Bean
  SecurityWebFilterChain gatewaySecurityFilterChain(
      ServerHttpSecurity http,
      JwtRoleConverter jwtRoleConverter,
      GatewaySecurityErrorHandler securityErrorHandler) {
    // JWT 기반 API는 서버 세션이나 브라우저 로그인 화면을 사용하지 않는다.
    return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
        .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
        .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
        .authorizeExchange(
            exchange ->
                exchange
                    // Access Token이 없는 회원가입 요청을 Member Service로 전달한다.
                    .pathMatchers(POST, "/api/members/signup")
                    .permitAll()
                    // 만료된 Access Token으로도 재발급과 Refresh Token 폐기를 요청할 수 있다.
                    .pathMatchers(POST, "/api/auth/login", "/api/auth/refresh", "/api/auth/logout")
                    .permitAll()
                    // Kubernetes readiness/liveness probe는 인증 없이 접근해야 한다.
                    .pathMatchers("/actuator/health/**")
                    .permitAll()
                    // hasRole("SELLER")는 내부적으로 ROLE_SELLER 권한을 확인한다.
                    .pathMatchers("/api/seller/**")
                    .hasRole("SELLER")
                    .pathMatchers("/api/members/**", "/api/core/**")
                    .hasAnyRole("MEMBER", "SELLER")
                    // 실수로 새 API가 무인증 공개되는 것을 막는 기본 거부 정책이다.
                    .anyExchange()
                    .denyAll())
        // 인가 규칙 단계에서 발생한 인증/권한 오류의 응답 형식을 통일한다.
        .exceptionHandling(
            exceptions ->
                exceptions
                    .authenticationEntryPoint(securityErrorHandler)
                    .accessDeniedHandler(securityErrorHandler))
        // Authorization: Bearer 값을 JWT로 해석하고 Decoder와 역할 Converter를 연결한다.
        .oauth2ResourceServer(
            oauth2 ->
                oauth2
                    .authenticationEntryPoint(securityErrorHandler)
                    .accessDeniedHandler(securityErrorHandler)
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtRoleConverter)))
        .build();
  }
}
