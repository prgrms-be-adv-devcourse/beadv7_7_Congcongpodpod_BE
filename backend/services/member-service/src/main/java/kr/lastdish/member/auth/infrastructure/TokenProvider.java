package kr.lastdish.member.auth.infrastructure;

import kr.lastdish.member.member.domain.Role;

public interface TokenProvider {
  String createAccessToken(Long memberId, Role role);

  String createRefreshToken();
}
