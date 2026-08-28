package kr.lastdish.ai.foodclassify.config;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

  @Bean
  public RestClient restClient() {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout((int) Duration.ofSeconds(3).toMillis());
    requestFactory.setReadTimeout((int) Duration.ofSeconds(3).toMillis());

    return RestClient.builder()
        .requestFactory(requestFactory)
        .defaultHeader("ngrok-skip-browser-warning", "69420")
        .build();
  }
}
