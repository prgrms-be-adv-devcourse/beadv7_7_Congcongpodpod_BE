package kr.lastdish.gateway.security;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

@SpringBootTest
@Import(GatewaySecurityConfigTests.TestRoutes.class)
class GatewaySecurityConfigTests {

  @Autowired ApplicationContext applicationContext;

  WebTestClient webTestClient;

  @BeforeEach
  void setUp() {
    webTestClient =
        WebTestClient.bindToApplicationContext(applicationContext).apply(springSecurity()).build();
  }

  @Test
  void loginRouteAllowsRequestsWithoutAuthentication() {
    webTestClient.post().uri("/api/auth/login").exchange().expectStatus().isOk();
  }

  @Test
  void protectedRouteRejectsRequestsWithoutAuthentication() {
    webTestClient
        .get()
        .uri("/api/core/test")
        .exchange()
        .expectStatus()
        .isUnauthorized()
        .expectHeader()
        .contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$.code")
        .isEqualTo("INVALID_ACCESS_TOKEN")
        .jsonPath("$.message")
        .isEqualTo("인증이 필요합니다.");
  }

  @Test
  void protectedRouteAllowsRequestsWithJwtAuthentication() {
    webTestClient
        .mutateWith(
            mockJwt()
                .jwt(jwt -> jwt.subject("1"))
                .authorities(new SimpleGrantedAuthority("ROLE_MEMBER")))
        .get()
        .uri("/api/core/test")
        .exchange()
        .expectStatus()
        .isOk();
  }

  @Test
  void memberCannotAccessSellerRoute() {
    webTestClient
        .mutateWith(
            mockJwt()
                .jwt(jwt -> jwt.subject("1"))
                .authorities(new SimpleGrantedAuthority("ROLE_MEMBER")))
        .get()
        .uri("/api/seller/test")
        .exchange()
        .expectStatus()
        .isForbidden()
        .expectHeader()
        .contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$.code")
        .isEqualTo("ACCESS_DENIED")
        .jsonPath("$.message")
        .isEqualTo("접근 권한이 없습니다.");
  }

  @Test
  void sellerCanAccessSellerRoute() {
    webTestClient
        .mutateWith(
            mockJwt()
                .jwt(jwt -> jwt.subject("2"))
                .authorities(new SimpleGrantedAuthority("ROLE_SELLER")))
        .get()
        .uri("/api/seller/test")
        .exchange()
        .expectStatus()
        .isOk();
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class TestRoutes {

    @Bean
    RouterFunction<ServerResponse> securityTestRoutes() {
      return route(POST("/api/auth/login"), request -> ok().build())
          .andRoute(GET("/api/core/test"), request -> ok().build())
          .andRoute(GET("/api/seller/test"), request -> ok().build());
    }
  }
}
