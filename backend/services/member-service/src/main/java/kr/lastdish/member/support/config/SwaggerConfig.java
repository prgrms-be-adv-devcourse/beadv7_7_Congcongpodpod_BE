package kr.lastdish.member.support.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI memberOpenAPI() {

        return new OpenAPI()
                .info(new Info()
                        .title("Member API")
                        .version("v1"))
                .servers(List.of(
                        new Server().url("/member")
                ));
//              jwt 도입 시 수정 (Authorize 설정)
//                .addSecurityItem(
//                        new SecurityRequirement().addList(securitySchemeName)
//                )
//                .components(new Components()
//                        .addSecuritySchemes(
//                                securitySchemeName,
//                                new SecurityScheme()
//                                        .name(securitySchemeName)
//                                        .type(SecurityScheme.Type.HTTP)
//                                        .scheme("bearer")
//                                        .bearerFormat("JWT")
//                        )
//                );
    }

}