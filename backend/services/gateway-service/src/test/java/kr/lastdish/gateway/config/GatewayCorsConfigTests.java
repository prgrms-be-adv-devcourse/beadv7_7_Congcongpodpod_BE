package kr.lastdish.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.config.GlobalCorsProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;

@SpringBootTest
class GatewayCorsConfigTests {

  @Autowired GlobalCorsProperties globalCorsProperties;

  @Test
  void allowsConfiguredWebOriginsAndApiRequestProperties() {
    CorsConfiguration configuration = apiCorsConfiguration();

    assertThat(configuration.checkOrigin("https://lastdish.kr")).isEqualTo("https://lastdish.kr");
    assertThat(configuration.checkOrigin("https://www.lastdish.kr"))
        .isEqualTo("https://www.lastdish.kr");
    assertThat(configuration.checkHttpMethod(HttpMethod.PATCH)).contains(HttpMethod.PATCH);
    assertThat(
            configuration.checkHeaders(
                List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE)))
        .containsExactly(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE);
  }

  @Test
  void rejectsUnknownWebOrigin() {
    assertThat(apiCorsConfiguration().checkOrigin("https://example.com")).isNull();
  }

  private CorsConfiguration apiCorsConfiguration() {
    return globalCorsProperties.getCorsConfigurations().get("/api/**");
  }
}
