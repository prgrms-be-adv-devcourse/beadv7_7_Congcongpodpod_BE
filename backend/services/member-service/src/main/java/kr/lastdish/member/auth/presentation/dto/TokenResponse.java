package kr.lastdish.member.auth.presentation.dto;

import kr.lastdish.member.auth.application.dto.TokenResult;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {
  private String accessToken;
  private String refreshToken;

  public static TokenResponse from(TokenResult result) {
    return new TokenResponse(result.accessToken(), result.refreshToken());
  }
}
