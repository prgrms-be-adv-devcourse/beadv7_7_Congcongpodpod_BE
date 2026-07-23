package kr.lastdish.member.auth.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TokenRefreshRequest {
  @NotBlank(message = "Refresh Token은 필수 입력 값입니다.")
  private String refreshToken;
}
