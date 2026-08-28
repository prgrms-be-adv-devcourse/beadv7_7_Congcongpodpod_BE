package kr.lastdish.payment.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class TossRestClientConfig {

  @Bean
  public RestClient.Builder tossRestClientBuilder() {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(3000);
    requestFactory.setReadTimeout(60000); // Toss 권장 Timeout 60초
    return RestClient.builder().requestFactory(requestFactory);
  }
}
