package kr.lastdish.ai.config;

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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Profile("local")
@Configuration(proxyBeanMethods = false)
public class SwaggerConfig {

  @Bean
  OpenAPI aiOpenApi(
      @Value("${swagger.servers.gateway-url}") String gatewayUrl,
      @Value("${swagger.servers.direct-url}") String directUrl) {
    return new OpenAPI()
        .info(new Info().title("LastDish AI API").version("v1"))
        .servers(
            List.of(
                new Server().url(gatewayUrl).description("Gateway 통합 테스트"),
                new Server().url(directUrl).description("AI Service 직접 테스트")))
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
  CorsFilter swaggerCorsConfigurer(@Value("${swagger.cors.allowed-origin}") String allowedOrigin) {
    CorsConfiguration config = new CorsConfiguration();
    config.addAllowedOrigin(allowedOrigin);
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.addAllowedHeader("*");

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/swagger-ui/**", config);
    source.registerCorsConfiguration("/swagger-ui.html", config);
    source.registerCorsConfiguration("/v3/api-docs/**", config);

    return new CorsFilter(source);
  }
}
