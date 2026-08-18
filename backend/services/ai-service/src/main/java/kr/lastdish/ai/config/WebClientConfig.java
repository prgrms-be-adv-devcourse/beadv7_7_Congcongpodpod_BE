package kr.lastdish.ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

  @Bean
  public WebClient webClient() {
    // 이미지 용량 처리를 위해 인메모리 버퍼 크기를 10MB로 확장
    ExchangeStrategies strategies =
        ExchangeStrategies.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
            .build();

    return WebClient.builder()
        .exchangeStrategies(strategies)
        .defaultHeader("ngrok-skip-browser-warning", "69420") // ngrok 우회 헤더
        .build();
  }
}
