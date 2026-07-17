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
        registry
            .addMapping("/**")
            .allowedOrigins(allowedOrigin)
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("*");
      }
    };
  }
}
