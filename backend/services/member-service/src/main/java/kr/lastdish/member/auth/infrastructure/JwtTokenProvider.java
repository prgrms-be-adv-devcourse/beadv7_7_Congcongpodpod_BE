package kr.lastdish.member.auth.infrastructure;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

  private final PrivateKey privateKey; // RSA 개인 키
  private final PublicKey publicKey; // RSA 공개 키 (게이트웨이 전달용 또는 검증용)
  private final long accessTokenValidityInMilliseconds;
  private final long refreshTokenValidityInMilliseconds;

  // 생성자를 통해 주입받도록 구성 (JwtConfig 등 활용)
  public JwtTokenProvider(
      PrivateKey privateKey,
      PublicKey publicKey,
      long accessTokenValidityInSeconds,
      long refreshTokenValidityInSeconds) {
    this.privateKey = privateKey;
    this.publicKey = publicKey;
    this.accessTokenValidityInMilliseconds = accessTokenValidityInSeconds * 1000;
    this.refreshTokenValidityInMilliseconds = refreshTokenValidityInSeconds * 1000;
  }

  public String createAccessToken(String email) {
    Date now = new Date();
    Date validity = new Date(now.getTime() + accessTokenValidityInMilliseconds);

    return Jwts.builder()
        .setSubject(email)
        .setIssuedAt(now)
        .setExpiration(validity)
        .signWith(privateKey) // RSA 개인 키로 서명
        .compact();
  }

  public String createRefreshToken(String email) {
    Date now = new Date();
    Date validity = new Date(now.getTime() + refreshTokenValidityInMilliseconds);

    return Jwts.builder()
        .setSubject(email)
        .setIssuedAt(now)
        .setExpiration(validity)
        .signWith(privateKey) // RSA 개인 키로 서명
        .compact();
  }

  public boolean validateToken(String token) {
    try {
      Jwts.parserBuilder().setSigningKey(publicKey).build().parseClaimsJws(token);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public String getEmail(String token) {
    Claims claims =
        Jwts.parserBuilder().setSigningKey(publicKey).build().parseClaimsJws(token).getBody();
    return claims.getSubject();
  }
}
