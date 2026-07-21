package kr.lastdish.member.auth.infrastructure;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

  private final Key key;
  private final long accessTokenValidityInMilliseconds;
  private final long refreshTokenValidityInMilliseconds;

  public JwtTokenProvider(
      @Value("${jwt.secret}") String secretKey,
      @Value("${jwt.access-token-validity-in-seconds}") long accessTokenValidityInSeconds,
      @Value("${jwt.refresh-token-validity-in-seconds}") long refreshTokenValidityInSeconds) {
    this.key = Keys.hmacShaKeyFor(secretKey.getBytes());
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
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();
  }

  public String createRefreshToken(String email) {
    Date now = new Date();
    Date validity = new Date(now.getTime() + refreshTokenValidityInMilliseconds);

    return Jwts.builder()
        .setSubject(email)
        .setIssuedAt(now)
        .setExpiration(validity)
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();
  }

  public boolean validateToken(String token) {
    try {
      Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public String getEmail(String token) {
    Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    return claims.getSubject();
  }
}
