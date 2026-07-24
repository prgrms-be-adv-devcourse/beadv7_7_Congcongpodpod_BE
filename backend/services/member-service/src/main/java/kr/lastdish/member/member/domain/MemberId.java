package kr.lastdish.member.member.domain;

import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.common.api.exception.CommonErrorCode;
import kr.lastdish.member.member.exception.ErrorCode;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class MemberId {
  private final Long value;

  public MemberId(Long value) {
    if (value == null) {
      throw new BusinessException(CommonErrorCode.INVALID_INPUT, "회원 ID는 null일 수 없습니다.");
    }
    this.value = value;
  }

  public MemberId(String raw) {
    try {
      this.value = Long.parseLong(raw);
    } catch (NumberFormatException e) {
      throw new BusinessException(ErrorCode.INVALID_TOKEN, "유효하지 않은 토큰입니다.");
    }
  }
}
