package kr.lastdish.member.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import kr.lastdish.member.auth.domain.RefreshToken;
import kr.lastdish.member.auth.domain.RefreshTokenRepository;
import kr.lastdish.member.auth.infrastructure.JwtTokenProvider;
import kr.lastdish.member.auth.presentation.dto.*;
import kr.lastdish.member.member.domain.Member;
import kr.lastdish.member.member.domain.MemberRepository;
import kr.lastdish.member.member.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceTest {

  @Autowired private AuthService authService;

  @Autowired private MemberRepository memberRepository;

  @Autowired private RefreshTokenRepository refreshTokenRepository;

  @Test
  @DisplayName("로그인 시 리프레시 토큰이 평문이 아니라 SHA-256 해시값으로 DB에 저장된다.")
  void loginSavesHashedRefreshToken() {
    // given
    SignUpRequest signUpRequest =
        new SignUpRequest(
            "hashtestuser", "password123!", "해시테스터", "010-9999-8888", "hash@example.com", "MEMBER");
    authService.signUp(signUpRequest);

    LoginRequest loginRequest = new LoginRequest("hash@example.com", "password123!");

    // when
    TokenResponse tokenResponse = authService.login(loginRequest);

    // then
    RefreshToken savedToken = refreshTokenRepository.findByEmail("hash@example.com").orElseThrow();

    // DB에 저장된 토큰 값은 클라이언트에게 준 원본 토큰(tokenResponse.getRefreshToken())과 달라야 함 (해시되었으므로)
    assertThat(savedToken.getToken()).isNotEqualTo(tokenResponse.getRefreshToken());
    // 해시값 길이 확인 (SHA-256은 16진수 64자리)
    assertThat(savedToken.getToken()).hasSize(64);
  }

  @Test
  @DisplayName("유효한 리프레시 토큰으로 재발급을 요청하면 새로운 Access Token이 발급된다.")
  void reissueSuccess() {
    // given
    SignUpRequest signUpRequest =
        new SignUpRequest(
            "reissueuser",
            "password123!",
            "재발급테스터",
            "010-1111-2222",
            "reissue@example.com",
            "MEMBER");
    authService.signUp(signUpRequest);

    TokenResponse initialTokens =
        authService.login(new LoginRequest("reissue@example.com", "password123!"));

    // when (ReissueRequest 객체로 감싸서 전달)
    TokenResponse newTokens =
        authService.refresh(new TokenRefreshRequest(initialTokens.getRefreshToken()));

    // then
    assertThat(newTokens.getAccessToken()).isNotNull();
    assertThat(newTokens.getRefreshToken()).isNotNull();
    assertThat(newTokens.getRefreshToken()).isNotEqualTo(initialTokens.getRefreshToken());
  }

  @Test
  @DisplayName("위조되거나 변조된 리프레시 토큰으로 재발급을 요청하면 예외가 발생한다.")
  void reissueFailWithInvalidToken() {
    // given
    SignUpRequest signUpRequest =
        new SignUpRequest(
            "failuser", "password123!", "실패테스터", "010-3333-4444", "fail@example.com", "MEMBER");
    authService.signUp(signUpRequest);
    authService.login(new LoginRequest("fail@example.com", "password123!"));

    String invalidRefreshToken = "invalid.token.string";

    // when & then (ReissueRequest 객체로 감싸서 전달)
    assertThatThrownBy(() -> authService.refresh(new TokenRefreshRequest(invalidRefreshToken)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("유효하지 않은 Refresh Token입니다.");
  }

  @Autowired private JwtTokenProvider jwtTokenProvider;

  @Test
  @DisplayName("만료된 리프레시 토큰으로 재발급을 요청하면 예외가 발생한다.")
  void reissueFailWithExpiredToken() {
    // given
    SignUpRequest signUpRequest =
        new SignUpRequest(
            "expireduser",
            "password123!",
            "만료테스터",
            "010-5555-6666",
            "expired@example.com",
            "MEMBER");
    authService.signUp(signUpRequest);
    authService.login(new LoginRequest("expired@example.com", "password123!"));

    Member member = memberRepository.findByEmail("expired@example.com").orElseThrow();

    String expiredToken =
        jwtTokenProvider.createExpiredRefreshToken(
            new kr.lastdish.member.member.domain.MemberId(member.getId()), member.getRole());

    // when & then
    assertThatThrownBy(() -> authService.refresh(new TokenRefreshRequest(expiredToken)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("유효하지 않은 Refresh Token입니다.");
  }

  @Test
  @DisplayName("로그아웃을 요청하면 리프레시 토큰이 삭제되어 재발급 요청 시 예외가 발생한다.")
  void logoutSuccess() {
    // given
    SignUpRequest signUpRequest =
        new SignUpRequest(
            "logoutuser",
            "password123!",
            "로그아웃테스터",
            "010-7777-8888",
            "logout@example.com",
            "MEMBER");
    authService.signUp(signUpRequest);
    TokenResponse tokens =
        authService.login(new LoginRequest("logout@example.com", "password123!"));

    // when
    authService.logout(new TokenLogoutRequest(tokens.getRefreshToken()));

    // then
    assertThatThrownBy(() -> authService.refresh(new TokenRefreshRequest(tokens.getRefreshToken())))
        .isInstanceOf(BusinessException.class);
  }
}
