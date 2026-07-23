package kr.lastdish.member.auth.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {
  private String accessToken;
  private String refreshToken;

  public static TokenResponse of(String accessToken, String refreshToken) {
    return new TokenResponse(accessToken, refreshToken);
  }
}
