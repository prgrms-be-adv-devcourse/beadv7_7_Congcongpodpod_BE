package kr.lastdish.member.auth.presentation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import kr.lastdish.member.auth.application.dto.SignUpCommand;

public record SignUpRequest(
    @NotBlank(message = "아이디는 필수 입력값입니다.")
        @Size(min = 4, max = 20, message = "아이디는 4자 이상 20자 이하로 입력해 주세요.")
        String userName,
    @NotBlank(message = "비밀번호는 필수 입력값입니다.") @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
        String password,
    @NotBlank(message = "이름은 필수 입력값입니다.") String name,
    @NotBlank(message = "전화번호는 필수 입력값입니다.") String phone,
    @NotBlank(message = "이메일은 필수 입력값입니다.") @Email(message = "이메일 형식이 올바르지 않습니다.") String email,
    @NotBlank(message = "역할(ROLE)은 필수 입력값입니다.") String role // "MEMBER" 또는 "SELLER"
    ) {

  public SignUpCommand toCommand() {
    return new SignUpCommand(
        userName,
        password,
        name,
        phone,
        email,
        role);
  }

}
