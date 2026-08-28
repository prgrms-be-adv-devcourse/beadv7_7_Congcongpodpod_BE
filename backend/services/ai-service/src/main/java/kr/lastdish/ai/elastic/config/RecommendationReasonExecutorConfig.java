package kr.lastdish.ai.elastic.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RecommendationReasonExecutorConfig {

  @Bean(destroyMethod = "shutdown")
  public ExecutorService recommendationReasonExecutor() {
    return Executors.newFixedThreadPool(4);
  }
}
