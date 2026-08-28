package kr.lastdish.ai.elastic.infrastructure.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

  @Bean
  public ObjectMapper objectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    // LocalDateTime 등 Java 8 날짜/시간 타입 지원
    objectMapper.registerModule(new JavaTimeModule());
    // ISO-8601 포맷 타임스탬프 처리
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    // DTO에 없는 미정의 필드가 파싱 대상에 들어와도 에러 방지
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return objectMapper;
  }
}
