package kr.lastdish.member.auth.exception;

import kr.lastdish.common.api.exception.ErrorCodeSpec;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCodeSpec {
  EMAIL_NOT_FOUND(HttpStatus.NOT_FOUND, "A002", "존재하지 않는 이메일입니다."),
  INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "A003", "비밀번호가 일치하지 않습니다."),
  INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "A004", "유효하지 않은 Refresh Token입니다."),
  REFRESH_TOKEN_MISMATCH(HttpStatus.UNAUTHORIZED, "A005", "Refresh Token 정보가 일치하지 않습니다."),
  DUPLICATE_USERNAME(HttpStatus.CONFLICT, "A006", "이미 사용 중인 아이디입니다."),
  DUPLICATE_EMAIL(HttpStatus.CONFLICT, "A007", "이미 등록된 이메일입니다."),
  LOGOUT_FAILED(HttpStatus.UNAUTHORIZED, "A009", "이미 로그아웃되었거나 유효하지 않은 토큰입니다."),
  MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "A010", "존재하지 않는 회원입니다."),
  ALREADY_WITHDRAWN_MEMBER(HttpStatus.BAD_REQUEST, "A011", "이미 탈퇴한 회원입니다."),
  KAKAO_AUTH_FAILED(HttpStatus.BAD_REQUEST, "A012", "카카오 인증 처리에 실패했습니다."),
  KAKAO_UNLINK_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "A013", "카카오 연동 해제 중 오류가 발생했습니다."),
  SOCIAL_MEMBER_LOGIN_RESTRICTED(HttpStatus.BAD_REQUEST, "A014", "소셜 가입 계정입니다. 소셜 로그인을 이용하세요.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
