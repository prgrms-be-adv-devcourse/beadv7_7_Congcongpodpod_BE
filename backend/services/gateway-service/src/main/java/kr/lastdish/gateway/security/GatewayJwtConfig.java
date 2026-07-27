package kr.lastdish.gateway.security;

import java.io.IOException;
import java.io.InputStream;
import java.security.interfaces.RSAPublicKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

/**
 * Gateway가 사용할 Reactive JWT Decoder를 구성한다.
 *
 * <p>Member Service는 Access 개인키로 토큰을 서명하고 Gateway는 그 키 쌍의 Access 공개키로 서명을 검증한다. 공개키로는 서명할 수 없으므로
 * Gateway가 토큰 발급 권한을 갖지 않는다.
 *
 * <p>이 Decoder는 요청마다 Member Service나 DB를 호출하지 않는다. 로컬에 주입된 공개키로 서명과 표준 시간 claim을 검증하고, 설정된 issuer와
 * 토큰의 {@code iss}가 일치하는지와 {@code token_type}이 {@code access}인지도 검사한다.
 */
@Configuration
public class GatewayJwtConfig {

  @Bean
  ReactiveJwtDecoder reactiveJwtDecoder(
      @Value("${gateway.security.jwt.public-key-location}") Resource publicKeyResource,
      @Value("${gateway.security.jwt.issuer}") String issuer)
      throws IOException {
    RSAPublicKey publicKey;

    // x509()은 "-----BEGIN PUBLIC KEY-----" 형식의 X.509 PEM을 RSAPublicKey로 변환한다.
    try (InputStream inputStream = publicKeyResource.getInputStream()) {
      publicKey = (RSAPublicKey) RsaKeyConverters.x509().convert(inputStream);
    }

    // Nimbus Decoder가 RS256 JWT의 서명을 비동기 WebFlux 방식으로 검증한다.
    NimbusReactiveJwtDecoder jwtDecoder = NimbusReactiveJwtDecoder.withPublicKey(publicKey).build();

    // 보호 API에는 서명/시간/issuer 검증을 통과한 Access Token만 사용할 수 있다.
    OAuth2TokenValidator<Jwt> defaultValidator = JwtValidators.createDefaultWithIssuer(issuer);
    OAuth2TokenValidator<Jwt> accessTokenValidator =
        new JwtClaimValidator<>("token_type", "access"::equals);
    jwtDecoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(defaultValidator, accessTokenValidator));
    return jwtDecoder;
  }
}
