package kr.lastdish.core.support.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfig {

  @Bean
  Clock businessClock() {
    return Clock.system(ZoneId.of("Asia/Seoul"));
  }
}
