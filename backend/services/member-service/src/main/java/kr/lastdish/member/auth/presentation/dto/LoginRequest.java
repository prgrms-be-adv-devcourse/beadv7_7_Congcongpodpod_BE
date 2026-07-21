package kr.lastdish.member.auth.presentation.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LoginRequest {
  private String email;
  private String password;
}
