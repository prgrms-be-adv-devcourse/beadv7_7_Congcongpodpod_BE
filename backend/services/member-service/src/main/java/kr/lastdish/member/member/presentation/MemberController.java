package kr.lastdish.member.member.presentation;

import kr.lastdish.common.api.response.ApiResponse;
import kr.lastdish.member.member.application.MemberService;
import kr.lastdish.member.member.application.dto.MemberProfileResult;
import kr.lastdish.member.member.presentation.dto.MemberProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

  private final MemberService memberService;

  @GetMapping("/me")
  public ApiResponse<MemberProfileResponse> getMyProfile(
      @RequestHeader("X-Authenticated-Member-Id") Long memberId) {
    MemberProfileResult result = memberService.getMemberById(memberId);
    return ApiResponse.ok(MemberProfileResponse.from(result));
  }

  @PatchMapping("/me")
  public ApiResponse<Void> withdrawMember(
      @RequestHeader("X-Authenticated-Member-Id") Long memberId) {
    memberService.withdrawMember(memberId);
    return ApiResponse.ok(null);
  }
}
