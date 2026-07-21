package kr.lastdish.member.auth.presentation.dto;

public record SignUpResponse(Long id, String userName, String email, String message) {
  public static SignUpResponse of(Long id, String userName, String email) {
    return new SignUpResponse(id, userName, email, "회원가입이 완료되었습니다.");
  }
}
