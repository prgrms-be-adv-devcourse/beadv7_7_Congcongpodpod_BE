package kr.lastdish.member.auth.config; // 프로젝트 패키지 구조에 맞춰주세요

import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {

  // 환경 변수 JWT_SECRET을 자동으로 읽어옵니다.
  @Value("${JWT_SECRET}")
  private String secret;

  @Bean
  public Key jwtSecretKey() {
    byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
    return Keys.hmacShaKeyFor(keyBytes);
  }
}
