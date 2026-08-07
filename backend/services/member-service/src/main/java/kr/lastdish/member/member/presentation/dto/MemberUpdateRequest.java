package kr.lastdish.member.member.presentation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MemberUpdateRequest {

  @Size(min = 4, max = 50, message = "유저네임은 4자 이상 50자 이하로 입력해주세요.")
  private String userName;

  @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
  private String password;

  @Size(max = 50, message = "이름은 최대 50자까지 입력 가능합니다.")
  private String name;

  @Size(max = 50, message = "전화번호 형식을 확인해주세요.")
  private String phone;

  @Email(message = "올바른 이메일 형식이어야 합니다.")
  @Size(max = 100, message = "이메일은 최대 100자까지 입력 가능합니다.")
  private String email;
}
