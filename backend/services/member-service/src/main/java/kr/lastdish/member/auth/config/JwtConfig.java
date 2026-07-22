package kr.lastdish.member.auth.config;

import java.io.InputStream;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.converter.RsaKeyConverters;

@Configuration
public class JwtConfig {

  @Value("${jwt.rsa.access-private-key-location}")
  private Resource privateKeyResource;

  @Value("${jwt.rsa.access-public-key-location}")
  private Resource publicKeyResource;

  @Value("${jwt.access-token-validity-in-seconds:1800}")
  private long accessTokenValidityInSeconds;

  @Value("${jwt.refresh-token-validity-in-seconds:1209600}")
  private long refreshTokenValidityInSeconds;

  @Bean
  public KeyPair jwtKeyPair() {


    System.out.println("PWD = " + new java.io.File(".").getAbsolutePath());
    System.out.println("Private = " + privateKeyResource);
    System.out.println("Public  = " + publicKeyResource);


    try (
            InputStream privateIs = privateKeyResource.getInputStream();
            InputStream publicIs = publicKeyResource.getInputStream()) {

      RSAPrivateKey privateKey =
              (RSAPrivateKey) RsaKeyConverters.pkcs8().convert(privateIs);

      RSAPublicKey publicKey =
              (RSAPublicKey) RsaKeyConverters.x509().convert(publicIs);

      return new KeyPair(publicKey, privateKey);

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
    return accessTokenValidityInSeconds;
  }

  @Bean
  public long refreshTokenValidityInSeconds() {
    return refreshTokenValidityInSeconds;
  }
}