package kr.lastdish.core.support.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI coreOpenAPI() {

        return new OpenAPI()
                .info(new Info()
                        .title("Core API")
                        .version("v1"))
                .servers(List.of(
                        new Server().url("/")
                ));
    }

}
