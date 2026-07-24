package kr.lastdish.member.member.presentation;

import kr.lastdish.common.api.response.ApiResponse;
import kr.lastdish.member.member.application.MemberService;
import kr.lastdish.member.member.application.dto.MemberProfileResult;
import kr.lastdish.member.member.presentation.dto.MemberProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

  private final MemberService memberService;

  @GetMapping("/me")
  public ApiResponse<MemberProfileResponse> getMyProfile(@AuthenticationPrincipal Long memberId) {
    MemberProfileResult result =
        memberService.getMemberById(memberId);
    return ApiResponse.ok(MemberProfileResponse.from(result));
  }

  @PatchMapping("/me")
  public ApiResponse<Void> withdrawMember(@AuthenticationPrincipal Long memberId) {
    memberService.withdrawMember(memberId);
    return ApiResponse.ok(null); // 또는 ApiResponse.success(null) 프로젝트 컨벤션에 맞게 사용
  }
}
