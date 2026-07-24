package kr.lastdish.member.auth.domain;

import kr.lastdish.member.member.domain.MemberId;
import kr.lastdish.member.member.domain.Role;

public interface TokenProvider {

  String createAccessToken(MemberId memberId, Role role);

  String createRefreshToken(MemberId memberId, Role role);

  boolean validateToken(String token);

  boolean isAccessToken(String token);

  boolean isRefreshToken(String token);

  MemberId getMemberId(String token);

  Role getRole(String token);
}