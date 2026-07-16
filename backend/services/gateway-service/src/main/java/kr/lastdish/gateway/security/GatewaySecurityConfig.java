package kr.lastdish.gateway.security;

import static org.springframework.http.HttpMethod.POST;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Gateway 라우팅 전에 인증과 역할 기반 접근 제어를 수행한다.
 *
 * <p>일반 API 요청마다 Member Service를 호출하지 않고 JWT를 Gateway에서 자체 검증한다.
 */
@Configuration
public class GatewaySecurityConfig {

  @Bean
  SecurityWebFilterChain gatewaySecurityFilterChain(
      ServerHttpSecurity http,
      JwtRoleConverter jwtRoleConverter,
      GatewaySecurityErrorHandler securityErrorHandler) {
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
                    .pathMatchers("/actuator/health/**")
                    .permitAll()
                    .pathMatchers("/api/seller/**")
                    .hasRole("SELLER")
                    .pathMatchers("/api/members/**", "/api/core/**")
                    .hasAnyRole("MEMBER", "SELLER")
                    .anyExchange()
                    .denyAll())
        .exceptionHandling(
            exceptions ->
                exceptions
                    .authenticationEntryPoint(securityErrorHandler)
                    .accessDeniedHandler(securityErrorHandler))
        .oauth2ResourceServer(
            oauth2 ->
                oauth2
                    .authenticationEntryPoint(securityErrorHandler)
                    .accessDeniedHandler(securityErrorHandler)
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtRoleConverter)))
        .build();
  }
}
