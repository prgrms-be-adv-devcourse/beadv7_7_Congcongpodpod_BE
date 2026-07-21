// package kr.lastdish.member.auth.config;
//
// import io.jsonwebtoken.security.Keys;
// import java.nio.charset.StandardCharsets;
// import java.security.Key;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
//
// @Configuration
// public class JwtConfig {
//
//  // application.yml의 jwt.secret 값을 읽어오도록 수정 (환경 변수 우선 적용도 원하신다면 ${JWT_SECRET:${jwt.secret}} 형태로
// 사용
//  // 가능)
//  @Value("${jwt.secret}")
//  private String secret;
//
//  @Bean
//  public Key jwtSecretKey() {
//    byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
//    return Keys.hmacShaKeyFor(keyBytes);
//  }
// }
package kr.lastdish.member.auth.config;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {

  @Bean
  public KeyPair jwtKeyPair() {
    try {
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
      keyPairGenerator.initialize(2048);
      return keyPairGenerator.generateKeyPair();
    } catch (Exception e) {
      throw new RuntimeException("JWT RSA 키페어 생성 실패", e);
    }
  }

  @Bean
  public PrivateKey privateKey(KeyPair jwtKeyPair) {
    return jwtKeyPair.getPrivate();
  }

  @Bean
  public PublicKey publicKey(KeyPair jwtKeyPair) {
    return jwtKeyPair.getPublic();
  }

  @Bean
  public long accessTokenValidityInSeconds() {
    return 1800L; // AccessToken 유효시간: 30분 (1800초)
  }

  @Bean
  public long refreshTokenValidityInSeconds() {
    return 1209600L; // RefreshToken 유효시간: 2주 (1209600초)
  }
}
