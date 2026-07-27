package kr.lastdish.member.member.presentation;

import kr.lastdish.common.api.response.ApiResponse;
import kr.lastdish.member.member.application.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/members")
@RequiredArgsConstructor
public class InternalMemberController {

  private final MemberService memberService;

  // 이 API는 Gateway에 라우팅하지 않고 core-service에서만 호출한다.
  @PatchMapping("/{memberId}/seller")
  public ApiResponse<Void> promoteToSeller(@PathVariable Long memberId) {
    memberService.promoteToSeller(memberId);
    return ApiResponse.ok(null);
  }
}