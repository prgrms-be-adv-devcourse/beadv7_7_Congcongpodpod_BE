package kr.lastdish.member.member.exception;

import kr.lastdish.common.api.exception.ErrorCodeSpec;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MemberErrorCode implements ErrorCodeSpec {
  MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "존재하지 않는 회원입니다."),
  INVALID_MEMBER_ID(HttpStatus.UNAUTHORIZED, "M008", "유효하지 않은 토큰입니다."),
  ALREADY_WITHDRAWN_MEMBER(HttpStatus.BAD_REQUEST, "M010", "이미 탈퇴한 회원입니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
