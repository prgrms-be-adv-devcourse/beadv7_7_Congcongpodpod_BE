package kr.lastdish.member.auth.presentation;

import jakarta.validation.Valid;
import kr.lastdish.common.api.response.ApiResponse;
import kr.lastdish.member.auth.application.AuthService;
import kr.lastdish.member.auth.application.dto.SignUpResult;
import kr.lastdish.member.auth.application.dto.TokenResult;
import kr.lastdish.member.auth.presentation.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @PostMapping("/signup")
  public ApiResponse<SignUpResponse> signUp(@Valid @RequestBody SignUpRequest request) {

    SignUpResult result = authService.signUp(request.toCommand());
    return ApiResponse.ok(SignUpResponse.from(result));
  }

  @PostMapping("/login")
  public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {

    TokenResult result = authService.login(request.toCommand());
    return ApiResponse.ok(TokenResponse.from(result));
  }

  @PostMapping("/refresh")
  public ApiResponse<TokenResponse> reissue(@Valid @RequestBody TokenRefreshRequest request) {

    TokenResult result = authService.refresh(request.toCommand());
    return ApiResponse.ok(TokenResponse.from(result));
  }

  @PostMapping("/logout")
  public ApiResponse<Void> logout(
      @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
      @Valid @RequestBody TokenLogoutRequest request) {

    // "Bearer " 접두사 제거하여 토큰 값만 추출
    String accessToken = null;
    if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
      accessToken = authorizationHeader.substring(7);
    }

    authService.logout(accessToken, request.toCommand());
    return ApiResponse.ok();
  }

  @PatchMapping("/withdraw")
  public ApiResponse<Void> withdraw(
      @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
      @RequestHeader("X-Authenticated-Member-Id") Long memberId) {

    String accessToken = null;
    if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
      accessToken = authorizationHeader.substring(7);
    }

    authService.withdraw(accessToken, memberId);
    return ApiResponse.ok();
  }

  @PostMapping("/kakao")
  public ApiResponse<TokenResponse> kakaoLogin(@RequestParam("code") String code) {
    TokenResult result = authService.kakaoLogin(code);
    return ApiResponse.ok(TokenResponse.from(result));
  }
}
