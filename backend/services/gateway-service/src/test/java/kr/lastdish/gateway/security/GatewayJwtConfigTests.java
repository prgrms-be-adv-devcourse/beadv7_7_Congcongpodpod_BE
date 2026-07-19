package kr.lastdish.gateway.security;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import reactor.test.StepVerifier;

class GatewayJwtConfigTests {

  private static final String ISSUER = "lastdish-member-service";

  private KeyPair keyPair;
  private ReactiveJwtDecoder jwtDecoder;

  @BeforeEach
  void setUp() throws Exception {
    keyPair = generateKeyPair();

    jwtDecoder =
        new GatewayJwtConfig()
            .reactiveJwtDecoder(publicKeyResource((RSAPublicKey) keyPair.getPublic()), ISSUER);
  }

  @Test
  void acceptsValidJwt() throws Exception {
    String token = createToken(keyPair, ISSUER, Instant.now().plusSeconds(300));

    StepVerifier.create(jwtDecoder.decode(token))
        .assertNext(jwt -> assertThat(jwt.getSubject()).isEqualTo("1"))
        .verifyComplete();
  }

  @Test
  void rejectsExpiredJwt() throws Exception {
    String token = createToken(keyPair, ISSUER, Instant.now().minusSeconds(120));

    StepVerifier.create(jwtDecoder.decode(token))
        .expectError(JwtValidationException.class)
        .verify();
  }

  @Test
  void rejectsJwtWithInvalidIssuerOrSignature() throws Exception {
    String invalidIssuerToken =
        createToken(keyPair, "another-service", Instant.now().plusSeconds(300));

    StepVerifier.create(jwtDecoder.decode(invalidIssuerToken))
        .expectError(JwtValidationException.class)
        .verify();

    KeyPair anotherKeyPair = generateKeyPair();
    String invalidSignatureToken =
        createToken(anotherKeyPair, ISSUER, Instant.now().plusSeconds(300));

    StepVerifier.create(jwtDecoder.decode(invalidSignatureToken))
        .expectError(JwtException.class)
        .verify();
  }

  private KeyPair generateKeyPair() throws Exception {
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    return generator.generateKeyPair();
  }

  private Resource publicKeyResource(RSAPublicKey publicKey) {
    String encoded =
        Base64.getMimeEncoder(64, "\n".getBytes(UTF_8)).encodeToString(publicKey.getEncoded());

    String pem = "-----BEGIN PUBLIC KEY-----\n" + encoded + "\n-----END PUBLIC KEY-----\n";

    return new ByteArrayResource(pem.getBytes(UTF_8));
  }

  private String createToken(KeyPair signingKeyPair, String issuer, Instant expiresAt)
      throws Exception {
    JWTClaimsSet claims =
        new JWTClaimsSet.Builder()
            .issuer(issuer)
            .subject("1")
            .issueTime(Date.from(Instant.now().minusSeconds(600)))
            .expirationTime(Date.from(expiresAt))
            .build();

    SignedJWT signedJwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);

    signedJwt.sign(new RSASSASigner((RSAPrivateKey) signingKeyPair.getPrivate()));

    return signedJwt.serialize();
  }
}
