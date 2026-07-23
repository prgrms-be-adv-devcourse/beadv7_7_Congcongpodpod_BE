package kr.lastdish.member.auth.presentation;

import jakarta.validation.Valid;
import kr.lastdish.member.auth.application.AuthService;
import kr.lastdish.member.auth.presentation.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
  public ResponseEntity<SignUpResponse> signUp(@Valid @RequestBody SignUpRequest request) {
    SignUpResponse response = authService.signUp(request);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/login")
  public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
    TokenResponse response = authService.login(request);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/refresh")
  public ResponseEntity<TokenResponse> reissue(@Valid @RequestBody TokenRefreshRequest request) {
    TokenResponse response = authService.refresh(request);
    return ResponseEntity.ok(response);
  }
}
