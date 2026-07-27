package kr.lastdish.core.support.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Profile("local")
@Configuration(proxyBeanMethods = false)
public class SwaggerConfig {

  @Bean
  OpenAPI coreOpenApi(
      @Value("${swagger.servers.gateway-url}") String gatewayUrl,
      @Value("${swagger.servers.direct-url}") String directUrl) {
    return new OpenAPI()
        .info(new Info().title("LastDish Core API").version("v1"))
        .servers(
            List.of(
                new Server().url(gatewayUrl).description("Gateway 통합 테스트"),
                new Server().url(directUrl).description("Core Service 직접 테스트")))
        .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
        .components(
            new Components()
                .addSecuritySchemes(
                    "bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
  }

  @Bean
  WebMvcConfigurer swaggerCorsConfigurer(
      @Value("${swagger.cors.allowed-origin}") String allowedOrigin) {
    return new WebMvcConfigurer() {
      @Override
      public void addCorsMappings(CorsRegistry registry) {
        // Swagger UI 문서 엔드포인트만 대상으로 한다 — 전체 경로(`/**`)에 걸면 일반 API 호출까지
        // 이 origin 하나로 제한돼버려서, Gateway CORS 설정과 무관하게 막히는 문제가 있었다.
        registry
            .addMapping("/swagger-ui/**")
            .allowedOrigins(allowedOrigin)
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("*");
        registry
            .addMapping("/swagger-ui.html")
            .allowedOrigins(allowedOrigin)
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("*");
        registry
            .addMapping("/v3/api-docs/**")
            .allowedOrigins(allowedOrigin)
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("*");
      }
    };
  }
}
