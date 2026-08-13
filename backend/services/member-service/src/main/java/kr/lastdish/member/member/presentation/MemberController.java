package kr.lastdish.member.member.presentation;

import jakarta.validation.Valid;
import kr.lastdish.common.api.response.ApiResponse;
import kr.lastdish.member.member.application.MemberService;
import kr.lastdish.member.member.application.dto.MemberProfileResult;
import kr.lastdish.member.member.presentation.dto.MemberProfileResponse;
import kr.lastdish.member.member.presentation.dto.MemberUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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

  @PutMapping("/me")
  public ApiResponse<Void> updateMember(
      @RequestHeader("X-Authenticated-Member-Id") Long memberId,
      @RequestBody @Valid MemberUpdateRequest requestDto) {
    memberService.updateMember(memberId, requestDto);
    return ApiResponse.ok(null);
  }
}
