package kr.lastdish.member.auth.presentation.dto;

import kr.lastdish.member.auth.application.dto.SignUpResult;

public record SignUpResponse(Long id, String userName, String email, String message) {
  public static SignUpResponse from(SignUpResult result) {
    return new SignUpResponse(
        result.id(),
        result.userName(),
        result.email(),
        "회원가입이 완료되었습니다.");
  }
}
