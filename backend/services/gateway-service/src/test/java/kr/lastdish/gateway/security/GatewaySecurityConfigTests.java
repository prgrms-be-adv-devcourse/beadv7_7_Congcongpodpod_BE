package kr.lastdish.gateway.security;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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

  @ParameterizedTest
  @ValueSource(strings = {"https://lastdish.kr", "https://www.lastdish.kr"})
  void corsPreflightDoesNotRequireAuthentication(String origin) {
    webTestClient
        .options()
        .uri("/api/v1/auth/login")
        .header(HttpHeaders.ORIGIN, origin)
        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.POST.name())
        .exchange()
        .expectStatus()
        .isOk();
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
  void publicStoreRouteAllowsGetRequestsWithoutAuthentication() {
    webTestClient.get().uri("/api/v1/stores/1").exchange().expectStatus().isOk();
  }

  @Test
  void aiSearchRouteAllowsPostRequestsWithoutAuthentication() {
    webTestClient.post().uri("/api/v1/ai/search").exchange().expectStatus().isOk();
  }

  @Test
  void myStoreRouteRejectsRequestsWithoutAuthentication() {
    webTestClient.get().uri("/api/v1/stores/mine").exchange().expectStatus().isUnauthorized();
  }

  @Test
  void myDishRouteRejectsRequestsWithoutAuthentication() {
    webTestClient.get().uri("/api/v1/stores/1/dish").exchange().expectStatus().isUnauthorized();
  }

  @Test
  void myDishesRouteRejectsRequestsWithoutAuthentication() {
    webTestClient.get().uri("/api/v1/stores/1/dishes").exchange().expectStatus().isUnauthorized();
  }

  @Test
  void memberCannotAccessMyStoreRoute() {
    webTestClient
        .mutateWith(
            mockJwt()
                .jwt(jwt -> jwt.subject("1"))
                .authorities(new SimpleGrantedAuthority("ROLE_MEMBER")))
        .get()
        .uri("/api/v1/stores/mine")
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  /**
   * 신규 MEMBER가 SELLER 전용 조회(예: 매장 등록 전 `GET /stores/mine`)를 호출하면 브라우저에서 온 요청이므로 Origin 헤더가 붙는다. 이
   * 403 응답에 Access-Control-Allow-Origin이 없으면 브라우저가 응답을 CORS 에러로 가로채 실제 403 대신 원인 불명의 네트워크 에러로
   * 보여준다(2026-07-30 발견, troubleshooting-draft 참고).
   */
  @Test
  void memberDeniedMyStoreRouteStillGetsCorsHeaderForBrowserOrigin() {
    webTestClient
        .mutateWith(
            mockJwt()
                .jwt(jwt -> jwt.subject("1"))
                .authorities(new SimpleGrantedAuthority("ROLE_MEMBER")))
        .get()
        .uri("/api/v1/stores/mine")
        .header(HttpHeaders.ORIGIN, "https://www.lastdish.kr")
        .exchange()
        .expectStatus()
        .isForbidden()
        .expectHeader()
        .valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://www.lastdish.kr");
  }

  @Test
  void sellerCanAccessMyStoreRoute() {
    webTestClient
        .mutateWith(
            mockJwt()
                .jwt(jwt -> jwt.subject("2"))
                .authorities(new SimpleGrantedAuthority("ROLE_SELLER")))
        .get()
        .uri("/api/v1/stores/mine")
        .exchange()
        .expectStatus()
        .isOk();
  }

  @Test
  void sellerCanAccessMyDishRoute() {
    webTestClient
        .mutateWith(
            mockJwt()
                .jwt(jwt -> jwt.subject("2"))
                .authorities(new SimpleGrantedAuthority("ROLE_SELLER")))
        .get()
        .uri("/api/v1/stores/1/dish")
        .exchange()
        .expectStatus()
        .isOk();
  }

  @Test
  void memberCannotAccessMyDishesRoute() {
    webTestClient
        .mutateWith(
            mockJwt()
                .jwt(jwt -> jwt.subject("1"))
                .authorities(new SimpleGrantedAuthority("ROLE_MEMBER")))
        .get()
        .uri("/api/v1/stores/1/dishes")
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void sellerCanAccessMyDishesRoute() {
    webTestClient
        .mutateWith(
            mockJwt()
                .jwt(jwt -> jwt.subject("2"))
                .authorities(new SimpleGrantedAuthority("ROLE_SELLER")))
        .get()
        .uri("/api/v1/stores/1/dishes")
        .exchange()
        .expectStatus()
        .isOk();
  }

  @Test
  void unknownPublicRouteUsesGatewayErrorResponse() {
    webTestClient
        .get()
        .uri("/api/v1/dishes/unknown")
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectHeader()
        .contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$.success")
        .isEqualTo(false)
        .jsonPath("$.error.code")
        .isEqualTo("G004")
        .jsonPath("$.timestamp")
        .exists();
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
        .jsonPath("$.success")
        .isEqualTo(false)
        .jsonPath("$.data")
        .doesNotExist()
        .jsonPath("$.error.code")
        .isEqualTo("G001")
        .jsonPath("$.error.message")
        .isEqualTo("인증이 필요합니다.")
        .jsonPath("$.timestamp")
        .exists();
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
        .jsonPath("$.success")
        .isEqualTo(false)
        .jsonPath("$.data")
        .doesNotExist()
        .jsonPath("$.error.code")
        .isEqualTo("G002")
        .jsonPath("$.error.message")
        .isEqualTo("접근 권한이 없습니다.")
        .jsonPath("$.timestamp")
        .exists();
  }

  @Test
  void memberCanRegisterStore() {
    webTestClient
        .mutateWith(
            mockJwt()
                .jwt(jwt -> jwt.subject("1"))
                .authorities(new SimpleGrantedAuthority("ROLE_MEMBER")))
        .post()
        .uri("/api/v1/stores")
        .exchange()
        .expectStatus()
        .isOk();
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
  void imageClassificationRejectsRequestsWithoutAuthentication() {
    webTestClient.post().uri("/api/v1/ai/classify").exchange().expectStatus().isUnauthorized();
  }

  @ParameterizedTest
  @ValueSource(strings = {"ROLE_MEMBER", "ROLE_SELLER"})
  void authenticatedMemberCanClassifyFoodImage(String authority) {
    webTestClient
        .mutateWith(
            mockJwt()
                .jwt(jwt -> jwt.subject("1"))
                .authorities(new SimpleGrantedAuthority(authority)))
        .post()
        .uri("/api/v1/ai/classify")
        .exchange()
        .expectStatus()
        .isOk();
  }

  @Test
  void geocodingRejectsRequestsWithoutAuthentication() {
    webTestClient
        .get()
        .uri("/api/v1/locations/geocode?query=서울남부터미널역")
        .exchange()
        .expectStatus()
        .isUnauthorized();
  }

  @ParameterizedTest
  @ValueSource(strings = {"ROLE_MEMBER", "ROLE_SELLER"})
  void authenticatedUserCanGeocodeStoreAddress(String authority) {
    webTestClient
        .mutateWith(
            mockJwt()
                .jwt(jwt -> jwt.subject("1"))
                .authorities(new SimpleGrantedAuthority(authority)))
        .get()
        .uri("/api/v1/locations/geocode?query=서울남부터미널역")
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

  @Test
  void notificationRouteRejectsRequestsWithoutAuthentication() {
    webTestClient.get().uri("/api/v1/notifications").exchange().expectStatus().isUnauthorized();
  }

  @ParameterizedTest
  @ValueSource(strings = {"ROLE_MEMBER", "ROLE_SELLER"})
  void authenticatedUserCanAccessNotifications(String authority) {
    webTestClient
        .mutateWith(
            mockJwt()
                .jwt(jwt -> jwt.subject("1"))
                .authorities(new SimpleGrantedAuthority(authority)))
        .get()
        .uri("/api/v1/notifications")
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
          .andRoute(GET("/api/v1/stores/1"), request -> ok().build())
          .andRoute(GET("/api/v1/stores/mine"), request -> ok().build())
          .andRoute(GET("/api/v1/stores/1/dish"), request -> ok().build())
          .andRoute(GET("/api/v1/stores/1/dishes"), request -> ok().build())
          .andRoute(GET("/api/v1/orders/test"), request -> ok().build())
          .andRoute(POST("/api/v1/stores"), request -> ok().build())
          .andRoute(POST("/api/v1/stores/1/dishes"), request -> ok().build())
          .andRoute(POST("/api/v1/ai/classify"), request -> ok().build())
          .andRoute(POST("/api/v1/ai/search"), request -> ok().build())
          .andRoute(GET("/api/v1/locations/geocode"), request -> ok().build())
          .andRoute(GET("/api/v1/notifications"), request -> ok().build())
          .andRoute(POST("/api/v1/deposits/test"), request -> ok().build());
    }
  }
}
