package kr.lastdish.member.auth.infrastructure;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import kr.lastdish.member.member.domain.MemberId;
import kr.lastdish.member.member.domain.Role; // Role 임포트 확인
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenProvider jwtTokenProvider;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    // 1. Request Header에서 토큰 추출
    String token = parseBearerToken(request);

    // 2. 토큰 유효성 검사
    if (token != null && jwtTokenProvider.validateToken(token)) {
      // 3. 토큰에서 memberId 추출
      MemberId memberId = jwtTokenProvider.getMemberId(token);
      Long id = memberId.getValue();

      // 4. 토큰에서 Role(Enum)을 가져와서 name()으로 문자열 변환
      Role role = jwtTokenProvider.getRole(token);
      List<SimpleGrantedAuthority> authorities =
          List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));

      // 5. SecurityContext에 인증 정보 저장
      UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(id, null, authorities);
      authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

      SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    filterChain.doFilter(request, response);
  }

  private String parseBearerToken(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");
    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
      return bearerToken.substring(7);
    }
    return null;
  }
}
