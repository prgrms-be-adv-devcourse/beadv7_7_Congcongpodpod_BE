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

@Configuration
public class GatewayJwtConfig {

  @Bean
  ReactiveJwtDecoder reactiveJwtDecoder(
      @Value("${gateway.security.jwt.public-key-location}") Resource publicKeyResource,
      @Value("${gateway.security.jwt.issuer}") String issuer)
      throws IOException {
    RSAPublicKey publicKey;

    try (InputStream inputStream = publicKeyResource.getInputStream()) {
      publicKey = (RSAPublicKey) RsaKeyConverters.x509().convert(inputStream);
    }

    NimbusReactiveJwtDecoder jwtDecoder = NimbusReactiveJwtDecoder.withPublicKey(publicKey).build();

    jwtDecoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
    return jwtDecoder;
  }
}
