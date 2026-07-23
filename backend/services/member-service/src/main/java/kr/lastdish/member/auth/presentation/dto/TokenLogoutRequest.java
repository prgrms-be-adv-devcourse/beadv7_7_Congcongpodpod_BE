package kr.lastdish.member.auth.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TokenLogoutRequest {
  @NotBlank(message = "Refresh Token은 필수 입력 값입니다.")
  private String refreshToken;
}
