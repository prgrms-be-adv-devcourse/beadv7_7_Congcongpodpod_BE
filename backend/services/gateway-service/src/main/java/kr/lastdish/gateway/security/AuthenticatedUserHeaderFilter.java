package kr.lastdish.gateway.security;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 인증이 끝난 요청에 내부 서비스가 사용할 사용자 식별 헤더를 추가한다.
 *
 * <p>이 필터는 모든 라우트에 적용되는 {@link GlobalFilter}다. Spring Security가 Access Token을 검증하면 {@link
 * JwtAuthenticationToken}이 요청의 Principal로 저장되고, 이 필터는 검증된 JWT의 {@code sub}와 {@code role}만 내부 헤더로
 * 변환한다.
 *
 * <p>클라이언트가 같은 이름의 헤더를 직접 보낼 수 있으므로 기존 값은 인증 여부와 관계없이 먼저 제거한다. 따라서 Member/Core Service가 받는 해당 헤더는
 * Gateway가 검증한 값이거나 아예 존재하지 않는다.
 */
@NullMarked
@Component
public class AuthenticatedUserHeaderFilter implements GlobalFilter {

  static final String MEMBER_ID_HEADER = "X-Authenticated-Member-Id";
  static final String ROLE_HEADER = "X-Authenticated-Role";

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    // 외부 입력을 신뢰하지 않기 위해 Principal을 조회하기 전에 사용자 헤더부터 제거한다.
    ServerWebExchange sanitizedExchange = removeUntrustedHeaders(exchange);

    // WebFlux에서는 Principal 조회도 비동기이므로 Mono 연산으로 요청 변환 과정을 연결한다.
    return sanitizedExchange
        .getPrincipal()
        // 공개 API의 익명 Principal 등은 제외하고, JWT 인증이 완료된 경우만 처리한다.
        .ofType(JwtAuthenticationToken.class)
        .map(authentication -> addAuthenticatedHeaders(sanitizedExchange, authentication))
        // 인증 정보가 없으면 헤더만 제거된 요청을 그대로 다음 필터로 전달한다.
        .defaultIfEmpty(sanitizedExchange)
        .flatMap(chain::filter);
  }

  private ServerWebExchange removeUntrustedHeaders(ServerWebExchange exchange) {
    ServerHttpRequest request =
        exchange
            .getRequest()
            .mutate()
            .headers(
                headers -> {
                  headers.remove(MEMBER_ID_HEADER);
                  headers.remove(ROLE_HEADER);
                })
            .build();

    return exchange.mutate().request(request).build();
  }

  private ServerWebExchange addAuthenticatedHeaders(
      ServerWebExchange exchange, JwtAuthenticationToken authentication) {
    // sub는 회원 식별자, role은 Member Service와 합의한 단일 역할 claim이다.
    @Nullable String memberId = authentication.getToken().getSubject();
    @Nullable String role = authentication.getToken().getClaimAsString("role");

    // 잘못된 내부 헤더를 만들지 않도록 둘 중 하나라도 비어 있으면 전달하지 않는다.
    if (!StringUtils.hasText(memberId) || !StringUtils.hasText(role)) {
      return exchange;
    }

    ServerHttpRequest request =
        exchange
            .getRequest()
            .mutate()
            .headers(
                headers -> {
                  headers.set(MEMBER_ID_HEADER, memberId);
                  headers.set(ROLE_HEADER, role);
                })
            .build();

    return exchange.mutate().request(request).build();
  }
}
