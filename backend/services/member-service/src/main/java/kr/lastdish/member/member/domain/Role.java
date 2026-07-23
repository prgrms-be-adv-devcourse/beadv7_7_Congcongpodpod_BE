package kr.lastdish.member.member.domain;

import kr.lastdish.member.member.exception.InvalidTokenException;

public enum Role {
  MEMBER,
  SELLER;

  public static Role from(String raw) {
    try {
      return Role.valueOf(raw);
    } catch (IllegalArgumentException e) {
      throw new InvalidTokenException("유효하지 않은 role입니다: " + raw);
    }
  }
}
