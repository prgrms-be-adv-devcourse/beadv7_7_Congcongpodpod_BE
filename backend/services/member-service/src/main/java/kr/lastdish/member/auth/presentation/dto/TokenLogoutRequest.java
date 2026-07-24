package kr.lastdish.member.auth.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import kr.lastdish.member.auth.application.dto.RefreshTokenCommand;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TokenLogoutRequest {
  @NotBlank(message = "Refresh Token은 필수 입력 값입니다.")
  private String refreshToken;

  public RefreshTokenCommand toCommand() {
    return new RefreshTokenCommand(refreshToken);
  }
}
