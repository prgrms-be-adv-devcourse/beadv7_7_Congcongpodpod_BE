package kr.lastdish.member.auth.infrastructure;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;
import java.util.UUID;
import kr.lastdish.member.member.domain.MemberId;
import kr.lastdish.member.member.domain.Role;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

  private final PrivateKey privateKey;
  private final PublicKey publicKey;
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

  public String createAccessToken(MemberId memberId, Role role) {
    Date now = new Date();
    Date validity = new Date(now.getTime() + accessTokenValidityInMilliseconds);

    return Jwts.builder()
        .setId(UUID.randomUUID().toString())
        .setSubject(String.valueOf(memberId.getValue()))
        .claim("role", role.name())
        .setIssuer("lastdish-member-service")
        .setIssuedAt(now)
        .setExpiration(validity)
        .signWith(privateKey)
        .compact();
  }

  public String createRefreshToken(MemberId memberId, Role role) {
    Date now = new Date();
    Date validity = new Date(now.getTime() + refreshTokenValidityInMilliseconds);

    return Jwts.builder()
        .setId(UUID.randomUUID().toString())
        .setSubject(String.valueOf(memberId.getValue()))
        .claim("role", role.name())
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

  public MemberId getMemberId(String token) {
    Claims claims =
        Jwts.parserBuilder().setSigningKey(publicKey).build().parseClaimsJws(token).getBody();
    return new MemberId(claims.getSubject());
  }

  public Role getRole(String token) {
    Claims claims =
        Jwts.parserBuilder().setSigningKey(publicKey).build().parseClaimsJws(token).getBody();
    String roleStr = claims.get("role", String.class);
    return Role.from(roleStr);
  }
}
