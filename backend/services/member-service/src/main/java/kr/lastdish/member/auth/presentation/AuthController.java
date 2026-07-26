package kr.lastdish.member.auth.presentation;

import jakarta.validation.Valid;
import kr.lastdish.common.api.response.ApiResponse;
import kr.lastdish.member.auth.application.AuthService;
import kr.lastdish.member.auth.presentation.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @PostMapping("/signup")
  public ApiResponse<SignUpResponse> signUp(@Valid @RequestBody SignUpRequest request) {
    SignUpResponse response = authService.signUp(request);
    return ApiResponse.ok(response);
  }

  @PostMapping("/login")
  public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
    TokenResponse response = authService.login(request);
    return ApiResponse.ok(response);
  }

  @PostMapping("/refresh")
  public ApiResponse<TokenResponse> reissue(@Valid @RequestBody TokenRefreshRequest request) {
    TokenResponse response = authService.refresh(request);
    return ApiResponse.ok(response);
  }

  @PostMapping("/logout")
  public ApiResponse<Void> logout(@Valid @RequestBody TokenLogoutRequest request) {
    authService.logout(request);
    return ApiResponse.ok();
  }
}
