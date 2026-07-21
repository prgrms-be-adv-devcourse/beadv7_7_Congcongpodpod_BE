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
  private final PublicKey publicKey; // RSA 공개 키 (게이트웨이 검증용)
  private final long accessTokenValidityInMilliseconds;
  private final long refreshTokenValidityInMilliseconds;

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

  public String createAccessToken(Long memberId, String role) {
    Date now = new Date();
    Date validity = new Date(now.getTime() + accessTokenValidityInMilliseconds);

    return Jwts.builder()
        .setSubject(String.valueOf(memberId))
        .claim("role", role)
        .setIssuer("lastdish-member-service")
        .setIssuedAt(now)
        .setExpiration(validity)
        .signWith(privateKey)
        .compact();
  }

  public String createRefreshToken(Long memberId, String role) {
    Date now = new Date();
    Date validity = new Date(now.getTime() + refreshTokenValidityInMilliseconds);

    return Jwts.builder()
        .setSubject(String.valueOf(memberId))
        .claim("role", role)
        .setIssuer("lastdish-member-service")
        .setIssuedAt(now)
        .setExpiration(validity)
        .signWith(privateKey)
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

  public String getMemberId(String token) {
    Claims claims =
        Jwts.parserBuilder().setSigningKey(publicKey).build().parseClaimsJws(token).getBody();
    return claims.getSubject();
  }
}
