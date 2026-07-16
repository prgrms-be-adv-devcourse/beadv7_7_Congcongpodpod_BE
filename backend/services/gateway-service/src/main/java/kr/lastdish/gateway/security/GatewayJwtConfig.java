package kr.lastdish.gateway.security;

import java.io.IOException;
import java.io.InputStream;
import java.security.interfaces.RSAPublicKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

/**
 * Member Service가 개인키로 서명한 JWT를 공개키로 검증한다.
 *
 * <p>Gateway는 토큰 발급에 관여하지 않으며, issuer 검증으로 다른 서비스가 발급한 토큰을 차단한다.
 */
@Configuration
public class GatewayJwtConfig {

  @Bean
  ReactiveJwtDecoder reactiveJwtDecoder(
      @Value("${gateway.security.jwt.public-key-location}") Resource publicKeyResource,
      @Value("${gateway.security.jwt.issuer}") String issuer)
      throws IOException {
    RSAPublicKey publicKey;

    // Gateway에는 토큰 발급이 불가능한 공개키만 주입한다.
    try (InputStream inputStream = publicKeyResource.getInputStream()) {
      publicKey = (RSAPublicKey) RsaKeyConverters.x509().convert(inputStream);
    }

    NimbusReactiveJwtDecoder jwtDecoder = NimbusReactiveJwtDecoder.withPublicKey(publicKey).build();

    jwtDecoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
    return jwtDecoder;
  }
}
