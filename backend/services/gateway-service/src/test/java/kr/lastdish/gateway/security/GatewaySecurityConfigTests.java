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
    webTestClient.post().uri("/api/v1/auth/login").exchange().expectStatus().isOk();
  }

  @Test
  void openApiRouteAllowsRequestsWithoutAuthentication() {
    webTestClient.get().uri("/openapi/member-service").exchange().expectStatus().isOk();
  }

  @Test
  void publicDishRouteAllowsGetRequestsWithoutAuthentication() {
    webTestClient.get().uri("/api/v1/dishes/1").exchange().expectStatus().isOk();
  }

  @Test
  void protectedRouteRejectsRequestsWithoutAuthentication() {
    webTestClient
        .get()
        .uri("/api/v1/orders/test")
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
        .uri("/api/v1/orders/test")
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
        .post()
        .uri("/api/v1/stores/1/dishes")
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
        .post()
        .uri("/api/v1/stores/1/dishes")
        .exchange()
        .expectStatus()
        .isOk();
  }

  @Test
  void depositRouteRejectsRequestsWithoutAuthentication() {
    webTestClient.post().uri("/api/v1/deposits/test").exchange().expectStatus().isUnauthorized();
  }

  @Test
  void memberCanAccessDepositRoute() {
    webTestClient
        .mutateWith(
            mockJwt()
                .jwt(jwt -> jwt.subject("1"))
                .authorities(new SimpleGrantedAuthority("ROLE_MEMBER")))
        .post()
        .uri("/api/v1/deposits/test")
        .exchange()
        .expectStatus()
        .isOk();
  }

  @Test
  void sellerCanAccessDepositRoute() {
    webTestClient
        .mutateWith(
            mockJwt()
                .jwt(jwt -> jwt.subject("2"))
                .authorities(new SimpleGrantedAuthority("ROLE_SELLER")))
        .post()
        .uri("/api/v1/deposits/test")
        .exchange()
        .expectStatus()
        .isOk();
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class TestRoutes {

    @Bean
    RouterFunction<ServerResponse> securityTestRoutes() {
      return route(POST("/api/v1/auth/login"), request -> ok().build())
          .andRoute(GET("/openapi/member-service"), request -> ok().build())
          .andRoute(GET("/api/v1/dishes/1"), request -> ok().build())
          .andRoute(GET("/api/v1/orders/test"), request -> ok().build())
          .andRoute(POST("/api/v1/stores/1/dishes"), request -> ok().build())
          .andRoute(POST("/api/v1/deposits/test"), request -> ok().build());
    }
  }
}
