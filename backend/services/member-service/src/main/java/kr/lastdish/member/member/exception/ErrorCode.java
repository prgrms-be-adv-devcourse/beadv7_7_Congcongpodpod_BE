package kr.lastdish.member.member.exception;

import kr.lastdish.common.api.exception.ErrorCodeSpec;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode implements ErrorCodeSpec {

  // Auth / Member 관련 에러 코드
  MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "존재하지 않는 회원입니다."),
  EMAIL_NOT_FOUND(HttpStatus.NOT_FOUND, "M002", "존재하지 않는 이메일입니다."),
  INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "M003", "비밀번호가 일치하지 않습니다."),
  INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "M004", "유효하지 않은 Refresh Token입니다."),
  REFRESH_TOKEN_MISMATCH(HttpStatus.UNAUTHORIZED, "M005", "Refresh Token 정보가 일치하지 않습니다."),
  DUPLICATE_USERNAME(HttpStatus.CONFLICT, "M006", "이미 사용 중인 아이디입니다."),
  DUPLICATE_EMAIL(HttpStatus.CONFLICT, "M007", "이미 등록된 이메일입니다."),
  INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "M008", "유효하지 않은 토큰입니다."),
  LOGOUT_FAILED(HttpStatus.UNAUTHORIZED, "M009", "이미 로그아웃되었거나 유효하지 않은 토큰입니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
