package kr.lastdish.member.auth.infrastructure;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.security.Key;
import java.util.Date;
import kr.lastdish.member.member.domain.Role;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider implements TokenProvider {
  private final Key key; // 변수명을 privateKey -> key로 변경

  // 생성자 주입 시 @Qualifier가 필요할 수도 있습니다 (에러 발생 시 확인)
  public JwtTokenProvider(Key jwtSecretKey) {
    this.key = jwtSecretKey;
  }

  @Override
  public String createAccessToken(Long memberId, Role role) {
    return Jwts.builder()
        .setIssuer("lastdish-member-service")
        .setSubject(memberId.toString())
        .claim("role", role.name())
        .setExpiration(new Date(System.currentTimeMillis() + 3600000))
        .signWith(key, SignatureAlgorithm.HS256) // RS256 -> HS256으로 변경
        .compact();
  }

  @Override
  public String createRefreshToken() {
    return Jwts.builder()
        .setExpiration(new Date(System.currentTimeMillis() + 1209600000))
        .signWith(key, SignatureAlgorithm.HS256) // RS256 -> HS256으로 변경
        .compact();
  }
}
