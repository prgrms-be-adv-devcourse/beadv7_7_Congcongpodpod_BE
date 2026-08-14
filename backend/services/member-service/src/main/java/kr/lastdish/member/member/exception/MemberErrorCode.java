package kr.lastdish.member.member.exception;

import kr.lastdish.common.api.exception.ErrorCodeSpec;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MemberErrorCode implements ErrorCodeSpec {
  MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "존재하지 않는 회원입니다."),
  DUPLICATE_USERNAME(HttpStatus.CONFLICT, "M002", "이미 사용 중인 유저네임입니다."),
  DUPLICATE_EMAIL(HttpStatus.CONFLICT, "M003", "이미 사용 중인 이메일입니다."),
  INVALID_MEMBER_ID(HttpStatus.UNAUTHORIZED, "M004", "유효하지 않은 토큰입니다."),
  ALREADY_WITHDRAWN_MEMBER(HttpStatus.BAD_REQUEST, "M005", "이미 탈퇴한 회원입니다."),
  SOCIAL_MEMBER_CANNOT_CHANGE_PASSWORD(
      HttpStatus.BAD_REQUEST, "M006", "소셜 로그인 계정은 비밀번호를 변경할 수 없습니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
