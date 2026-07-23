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
 * Member Service의 {@code role} claim을 Spring Security의 GrantedAuthority로 변환한다.
 *
 * <p>JWT 원문에는 {@code MEMBER} 또는 {@code SELLER}가 들어오지만 Spring Security의 {@code hasRole} 규칙은 {@code
 * ROLE_} 접두사가 붙은 권한을 찾는다. 따라서 MEMBER는 ROLE_MEMBER, SELLER는 ROLE_SELLER로 변환한다.
 *
 * <p>기본 Converter는 동기 방식이므로 {@link ReactiveJwtAuthenticationConverterAdapter}로 감싸 WebFlux Security가
 * 요구하는 {@code Mono<AbstractAuthenticationToken>} 형태로 제공한다.
 */
@NullMarked
@Component
public class JwtRoleConverter implements Converter<Jwt, Mono<AbstractAuthenticationToken>> {

  private final ReactiveJwtAuthenticationConverterAdapter delegate;

  public JwtRoleConverter() {
    JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();

    // Spring 기본 claim인 scope/scp 대신 팀에서 합의한 role claim 하나를 읽는다.
    authoritiesConverter.setAuthoritiesClaimName("role");
    authoritiesConverter.setAuthorityPrefix("ROLE_");

    JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
    authenticationConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);

    delegate = new ReactiveJwtAuthenticationConverterAdapter(authenticationConverter);
  }

  @Override
  public Mono<AbstractAuthenticationToken> convert(Jwt jwt) {
    // 변환 결과에는 원본 Jwt와 ROLE_* 권한이 함께 들어가 이후 인가와 헤더 생성에 사용된다.
    return delegate.convert(jwt);
  }
}
