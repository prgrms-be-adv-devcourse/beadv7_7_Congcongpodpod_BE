package kr.lastdish.member.member.application.dto;

import java.time.LocalDateTime;
import kr.lastdish.member.member.domain.Member;

public record MemberProfileResult(
    Long id,
    String userName,
    String name,
    String phone,
    String email,
    String role,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {

  public static MemberProfileResult from(Member member) {
    return new MemberProfileResult(
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
