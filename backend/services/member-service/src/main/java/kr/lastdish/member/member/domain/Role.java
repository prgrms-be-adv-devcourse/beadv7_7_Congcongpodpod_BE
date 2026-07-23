package kr.lastdish.member.member.domain;

import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.common.api.exception.CommonErrorCode;

public enum Role {
  MEMBER,
  SELLER;

  public static Role from(String raw) {
    try {
      return Role.valueOf(raw);
    } catch (IllegalArgumentException e) {
      throw new BusinessException(CommonErrorCode.INVALID_INPUT, "유효하지 않은 role입니다: " + raw);
    }
  }
}
