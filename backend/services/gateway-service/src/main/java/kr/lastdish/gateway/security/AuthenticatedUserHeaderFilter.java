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

@NullMarked
@Component
public class AuthenticatedUserHeaderFilter implements GlobalFilter {

  static final String MEMBER_ID_HEADER = "X-Authenticated-Member-Id";
  static final String ROLE_HEADER = "X-Authenticated-Role";

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    ServerWebExchange sanitizedExchange = removeUntrustedHeaders(exchange);

    return sanitizedExchange
        .getPrincipal()
        .ofType(JwtAuthenticationToken.class)
        .map(authentication -> addAuthenticatedHeaders(sanitizedExchange, authentication))
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
    @Nullable String memberId = authentication.getToken().getSubject();
    @Nullable String role = authentication.getToken().getClaimAsString("role");

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
