package kr.lastdish.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class AiServiceApplication {
  public static void main(String[] args) {
    SpringApplication.run(AiServiceApplication.class, args);
  }
}
