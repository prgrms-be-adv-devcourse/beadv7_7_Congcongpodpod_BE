package kr.lastdish.member.member.presentation.dto;

import java.time.LocalDateTime;
import kr.lastdish.member.member.domain.Member;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

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

  public static MemberProfileResponse from(Member member) {
    return new MemberProfileResponse(
        member.getId(),
        member.getUserName(),
        member.getName(),
        member.getPhone(),
        member.getEmail(),
        member.getRole().name(),
        member.getCreatedAt(),
        member.getUpdatedAt());
  }
}
