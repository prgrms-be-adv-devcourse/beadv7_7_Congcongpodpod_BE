package kr.lastdish.gateway.security;

import org.jspecify.annotations.NullMarked;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Member Service의 role claim을 Spring Security 권한 형식으로 변환한다.
 *
 * <p>MEMBER는 ROLE_MEMBER, SELLER는 ROLE_SELLER로 변환된다.
 */
@NullMarked
@Component
public class JwtRoleConverter implements Converter<Jwt, Mono<AbstractAuthenticationToken>> {

  private final ReactiveJwtAuthenticationConverterAdapter delegate;

  public JwtRoleConverter() {
    JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();

    authoritiesConverter.setAuthoritiesClaimName("role");
    authoritiesConverter.setAuthorityPrefix("ROLE_");

    JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
    authenticationConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);

    delegate = new ReactiveJwtAuthenticationConverterAdapter(authenticationConverter);
  }

  @Override
  public Mono<AbstractAuthenticationToken> convert(Jwt jwt) {
    return delegate.convert(jwt);
  }
}
