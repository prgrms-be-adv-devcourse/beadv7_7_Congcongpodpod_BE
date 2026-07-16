package kr.lastdish.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@NullMarked
class AuthenticatedUserHeaderFilterTests {

  private final AuthenticatedUserHeaderFilter filter = new AuthenticatedUserHeaderFilter();

  @Test
  void replacesUntrustedHeadersWithAuthenticatedJwtValues() {
    MockServerHttpRequest request =
        MockServerHttpRequest.get("/api/core/test")
            .header(AuthenticatedUserHeaderFilter.MEMBER_ID_HEADER, "forged-member")
            .header(AuthenticatedUserHeaderFilter.ROLE_HEADER, "SELLER")
            .build();

    ServerWebExchange exchange = authenticatedExchange(request);

    CapturingFilterChain chain = new CapturingFilterChain();

    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

    HttpHeaders headers = chain.exchange().getRequest().getHeaders();

    assertThat(headers.getFirst(AuthenticatedUserHeaderFilter.MEMBER_ID_HEADER)).isEqualTo("123");
    assertThat(headers.getFirst(AuthenticatedUserHeaderFilter.ROLE_HEADER)).isEqualTo("MEMBER");
  }

  @Test
  void removesUntrustedHeadersFromUnauthenticatedRequests() {
    MockServerHttpRequest request =
        MockServerHttpRequest.post("/api/auth/login")
            .header(AuthenticatedUserHeaderFilter.MEMBER_ID_HEADER, "forged-member")
            .header(AuthenticatedUserHeaderFilter.ROLE_HEADER, "SELLER")
            .build();

    ServerWebExchange exchange = MockServerWebExchange.from(request);
    CapturingFilterChain chain = new CapturingFilterChain();

    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

    HttpHeaders headers = chain.exchange().getRequest().getHeaders();

    assertThat(headers.getFirst(AuthenticatedUserHeaderFilter.MEMBER_ID_HEADER)).isNull();

    assertThat(headers.getFirst(AuthenticatedUserHeaderFilter.ROLE_HEADER)).isNull();
  }

  private ServerWebExchange authenticatedExchange(MockServerHttpRequest request) {
    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .subject("123")
            .claim("role", "MEMBER")
            .build();

    JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);

    return MockServerWebExchange.from(request)
        .mutate()
        .principal(Mono.just(authentication))
        .build();
  }

  private static final class CapturingFilterChain implements GatewayFilterChain {

    private @Nullable ServerWebExchange exchange;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange) {
      this.exchange = exchange;
      return Mono.empty();
    }

    ServerWebExchange exchange() {
      return Objects.requireNonNull(exchange);
    }
  }
}
