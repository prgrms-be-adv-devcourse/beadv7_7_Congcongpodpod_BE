package kr.lastdish.member.auth.config;

import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {

  // application.yml의 jwt.secret 값을 읽어오도록 수정 (환경 변수 우선 적용도 원하신다면 ${JWT_SECRET:${jwt.secret}} 형태로 사용
  // 가능)
  @Value("${jwt.secret}")
  private String secret;

  @Bean
  public Key jwtSecretKey() {
    byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
    return Keys.hmacShaKeyFor(keyBytes);
  }
}
