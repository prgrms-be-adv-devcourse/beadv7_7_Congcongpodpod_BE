package kr.lastdish.member.auth.infrastructure;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;
import java.util.UUID;
import kr.lastdish.member.auth.domain.TokenProvider;
import kr.lastdish.member.member.domain.MemberId;
import kr.lastdish.member.member.domain.Role;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider implements TokenProvider {

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

  @Override
  public String createAccessToken(MemberId memberId, Role role) {
    Date now = new Date();
    Date validity = new Date(now.getTime() + accessTokenValidityInMilliseconds);

    return Jwts.builder()
        .id(UUID.randomUUID().toString())
        .subject(String.valueOf(memberId.getValue()))
        .claim("role", role.name())
        .claim("token_type", "access")
        .issuer("lastdish-member-service")
        .issuedAt(now)
        .expiration(validity)
        .signWith(privateKey)
        .compact();
  }

  @Override
  public String createRefreshToken(MemberId memberId, Role role) {
    Date now = new Date();
    Date validity = new Date(now.getTime() + refreshTokenValidityInMilliseconds);

    return Jwts.builder()
        .id(UUID.randomUUID().toString())
        .subject(String.valueOf(memberId.getValue()))
        .claim("role", role.name())
        .claim("token_type", "refresh")
        .issuer("lastdish-member-service")
        .issuedAt(now)
        .expiration(validity)
        .signWith(privateKey)
        .compact();
  }

  // 만료 리프레시 토큰 생성 (테스트 보조 기능이므로 따로 오버라이드 하지 않겠습니다!)
  public String createExpiredRefreshToken(MemberId memberId, Role role) {
    Date now = new Date();
    Date expiredAt = new Date(now.getTime() - 1000);

    return Jwts.builder()
        .id(UUID.randomUUID().toString())
        .subject(String.valueOf(memberId.getValue()))
        .claim("role", role.name())
        .claim("token_type", "refresh")
        .issuer("lastdish-member-service")
        .issuedAt(new Date(now.getTime() - 2000))
        .expiration(expiredAt)
        .signWith(privateKey)
        .compact();
  }

  @Override
  public boolean validateToken(String token) {
    try {
      parseClaims(token);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  // Access Token 여부 확인
  @Override
  public boolean isAccessToken(String token) {
    try {
      Claims claims = parseClaims(token);
      return "access".equals(claims.get("token_type"));
    } catch (Exception e) {
      return false;
    }
  }

  // Refresh Token 여부 확인
  @Override
  public boolean isRefreshToken(String token) {
    try {
      Claims claims = parseClaims(token);
      return "refresh".equals(claims.get("token_type"));
    } catch (Exception e) {
      return false;
    }
  }

  @Override
  public MemberId getMemberId(String token) {
    Claims claims = parseClaims(token);
    return new MemberId(claims.getSubject());
  }

  public Role getRole(String token) {
    Claims claims = parseClaims(token);
    String roleStr = claims.get("role", String.class);
    return Role.from(roleStr);
  }

  private Claims parseClaims(String token) {
    return Jwts.parser().verifyWith(publicKey).build().parseSignedClaims(token).getPayload();
  }
}
