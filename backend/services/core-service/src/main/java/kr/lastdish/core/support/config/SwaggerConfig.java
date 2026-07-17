package kr.lastdish.core.support.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

  @Bean
  public OpenAPI coreOpenAPI() {

    return new OpenAPI()
        .info(new Info().title("Core API").version("v1"))
        .servers(List.of(new Server().url("/")));
  }
}
