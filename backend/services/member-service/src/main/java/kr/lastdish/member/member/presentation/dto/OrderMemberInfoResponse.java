package kr.lastdish.member.member.presentation.dto;

import kr.lastdish.member.member.application.dto.MemberProfileResult;

public record OrderMemberInfoResponse(String name, String phone) {

  public static OrderMemberInfoResponse from(MemberProfileResult result) {
    return new OrderMemberInfoResponse(result.name(), result.phone());
  }
}
