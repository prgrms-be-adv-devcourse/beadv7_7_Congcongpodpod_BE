package kr.lastdish.core.geocoding.infrastructure;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
class NaverMapsClientConfig {

  private static final String BASE_URL = "https://maps.apigw.ntruss.com";

  @Bean
  RestClient naverMapsRestClient() {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(Duration.ofSeconds(3));
    requestFactory.setReadTimeout(Duration.ofSeconds(5));

    return RestClient.builder().baseUrl(BASE_URL).requestFactory(requestFactory).build();
  }
}
