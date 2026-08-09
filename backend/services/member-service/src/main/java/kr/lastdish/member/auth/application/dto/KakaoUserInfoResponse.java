package kr.lastdish.member.auth.application.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record KakaoUserInfoResponse(Long id, KakaoAccount kakaoAccount) {
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  public record KakaoAccount(String email, Profile profile) {}

  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  public record Profile(String nickname) {}

  public String nickname() {
    if (kakaoAccount != null && kakaoAccount.profile() != null) {
      return kakaoAccount.profile().nickname();
    }
    return "카카오사용자";
  }

  public String email() {
    if (kakaoAccount != null) {
      return kakaoAccount.email();
    }
    return null;
  }
}
