package kr.lastdish.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

@NullMarked
class JwtRoleConverterTests {

  private final JwtRoleConverter converter = new JwtRoleConverter();

  @ParameterizedTest
  @CsvSource({"MEMBER, ROLE_MEMBER", "SELLER, ROLE_SELLER"})
  void convertsRoleClaimToSpringAuthority(String role, String expectedAuthority) {
    Jwt jwt =
        Jwt.withTokenValue("token").header("alg", "RS256").subject("1").claim("role", role).build();

    AbstractAuthenticationToken authentication =
        Objects.requireNonNull(converter.convert(jwt).block());

    assertThat(authentication.getAuthorities())
        .extracting(GrantedAuthority::getAuthority)
        .contains(expectedAuthority);
  }
}
