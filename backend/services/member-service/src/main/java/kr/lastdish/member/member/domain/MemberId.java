package kr.lastdish.member.member.domain;

import kr.lastdish.member.member.exception.InvalidTokenException;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class MemberId {
  private final Long value;

  public MemberId(Long value) {
    if (value == null) {
      throw new InvalidTokenException("회원 ID는 null일 수 없습니다.");
    }
    this.value = value;
  }

  public MemberId(String raw) {
    try {
      this.value = Long.parseLong(raw);
    } catch (NumberFormatException e) {
      throw new InvalidTokenException("유효하지 않은 토큰입니다");
    }
  }
}
