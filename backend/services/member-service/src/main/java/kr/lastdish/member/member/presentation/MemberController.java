package kr.lastdish.member.member.presentation;

import kr.lastdish.common.api.response.ApiResponse;
import kr.lastdish.member.member.application.MemberService;
import kr.lastdish.member.member.presentation.dto.MemberProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

  private final MemberService memberService;

  @GetMapping("/me")
  public ApiResponse<MemberProfileResponse> getMyProfile(@AuthenticationPrincipal Long memberId) {
    MemberProfileResponse response = memberService.getMemberById(memberId);
    return ApiResponse.ok(response);
  }
}
