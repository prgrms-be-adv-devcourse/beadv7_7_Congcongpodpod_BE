package kr.lastdish.member.auth.config;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {

  @Value("${jwt.rsa.private-key:#{null}}")
  private String privateKeyPem;

  @Value("${jwt.rsa.public-key:#{null}}")
  private String publicKeyPem;

  @Bean
  public KeyPair jwtKeyPair() {
    try {
      // 설정 파일에 고정 키가 주입되지 않은 경우 (테스트 환경 등),
      // 런타임 에러를 방지하기 위해 자동으로 키페어를 동적 생성합니다.
      if (privateKeyPem == null
          || publicKeyPem == null
          || privateKeyPem.isBlank()
          || publicKeyPem.isBlank()) {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        return keyPairGenerator.generateKeyPair();
      }

      // 고정 키가 존재하는 경우 파싱하여 사용
      byte[] privBytes = Base64.getDecoder().decode(privateKeyPem);
      PKCS8EncodedKeySpec privSpec = new PKCS8EncodedKeySpec(privBytes);
      KeyFactory kf = KeyFactory.getInstance("RSA");
      PrivateKey privKey = kf.generatePrivate(privSpec);

      byte[] pubBytes = Base64.getDecoder().decode(publicKeyPem);
      X509EncodedKeySpec pubSpec = new X509EncodedKeySpec(pubBytes);
      PublicKey pubKey = kf.generatePublic(pubSpec);

      return new KeyPair(pubKey, privKey);
    } catch (Exception e) {
      throw new RuntimeException("RSA 키페어 초기화 실패", e);
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
    return 1800L;
  }

  @Bean
  public long refreshTokenValidityInSeconds() {
    return 1209600L;
  }
}
