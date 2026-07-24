package kr.lastdish.member.member.presentation.dto;

import kr.lastdish.member.member.application.dto.MemberProfileResult;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class MemberProfileResponse {

  private final Long id;
  private final String userName;
  private final String name;
  private final String phone;
  private final String email;
  private final String role;
  private final LocalDateTime createdAt;
  private final LocalDateTime updatedAt;

  public static MemberProfileResponse from(
      MemberProfileResult result) {

    return new MemberProfileResponse(
        result.id(),
        result.userName(),
        result.name(),
        result.phone(),
        result.email(),
        result.role(),
        result.createdAt(),
        result.updatedAt());
  }
}
