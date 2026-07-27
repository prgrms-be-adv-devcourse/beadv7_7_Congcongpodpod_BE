package kr.lastdish.core.support.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class MemberServiceClientConfig {

  @Bean
  RestClient memberServiceRestClient(@Value("${services.member.base-url}") String baseUrl) {

    return RestClient.builder().baseUrl(baseUrl).build();
  }
}
