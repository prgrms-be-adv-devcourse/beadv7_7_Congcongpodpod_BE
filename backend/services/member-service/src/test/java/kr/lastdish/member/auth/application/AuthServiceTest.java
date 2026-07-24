package kr.lastdish.member.auth.application;

import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.member.auth.application.dto.LoginCommand;
import kr.lastdish.member.auth.application.dto.RefreshTokenCommand;
import kr.lastdish.member.auth.application.dto.SignUpCommand;
import kr.lastdish.member.auth.application.dto.TokenResult;
import kr.lastdish.member.auth.domain.RefreshToken;
import kr.lastdish.member.auth.domain.RefreshTokenRepository;
import kr.lastdish.member.auth.infrastructure.JwtTokenProvider;
import kr.lastdish.member.auth.presentation.dto.*;
import kr.lastdish.member.member.domain.Member;
import kr.lastdish.member.member.domain.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    SignUpCommand command =
        new SignUpCommand(
            "hashtestuser", "password123!", "해시테스터", "010-9999-8888", "hash@example.com", "MEMBER");
    authService.signUp(command);

    LoginCommand loginCommand = new LoginCommand("hash@example.com", "password123!");

    // when
    TokenResult tokenResult = authService.login(loginCommand);

    // then
    RefreshToken savedToken = refreshTokenRepository.findByEmail("hash@example.com").orElseThrow();

    // DB에 저장된 토큰 값은 클라이언트에게 준 원본 토큰(tokenResponse.getRefreshToken())과 달라야 함 (해시되었으므로)
    assertThat(savedToken.getToken()).isNotEqualTo(tokenResult.refreshToken());
    // 해시값 길이 확인 (SHA-256은 16진수 64자리)
    assertThat(savedToken.getToken()).hasSize(64);
  }

  @Test
  @DisplayName("유효한 리프레시 토큰으로 재발급을 요청하면 새로운 Access Token이 발급된다.")
  void reissueSuccess() {
    // given
    SignUpCommand command =
        new SignUpCommand(
            "reissueuser",
            "password123!",
            "재발급테스터",
            "010-1111-2222",
            "reissue@example.com",
            "MEMBER");
    authService.signUp(command);

    TokenResult initialTokens =
        authService.login(new LoginCommand("reissue@example.com", "password123!"));

    // when (ReissueRequest 객체로 감싸서 전달)
    TokenResult newTokens =
        authService.refresh(new RefreshTokenCommand(initialTokens.refreshToken()));

    // then
    assertThat(newTokens.accessToken()).isNotNull();
    assertThat(newTokens.refreshToken()).isNotNull();
    assertThat(newTokens.refreshToken()).isNotEqualTo(initialTokens.refreshToken());
  }

  @Test
  @DisplayName("위조되거나 변조된 리프레시 토큰으로 재발급을 요청하면 예외가 발생한다.")
  void reissueFailWithInvalidToken() {
    // given
    SignUpCommand signUpCommand =
        new SignUpCommand(
            "failuser", "password123!", "실패테스터", "010-3333-4444", "fail@example.com", "MEMBER");
    authService.signUp(signUpCommand);
    authService.login(new LoginCommand("fail@example.com", "password123!"));

    String invalidRefreshToken = "invalid.token.string";

    // when & then (ReissueRequest 객체로 감싸서 전달)
    assertThatThrownBy(() -> authService.refresh(new RefreshTokenCommand(invalidRefreshToken)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("유효하지 않은 Refresh Token입니다.");
  }

  @Autowired private JwtTokenProvider jwtTokenProvider;

  @Test
  @DisplayName("만료된 리프레시 토큰으로 재발급을 요청하면 예외가 발생한다.")
  void reissueFailWithExpiredToken() {
    // given
    SignUpCommand signUpCommand =
        new SignUpCommand(
            "expireduser",
            "password123!",
            "만료테스터",
            "010-5555-6666",
            "expired@example.com",
            "MEMBER");
    authService.signUp(signUpCommand);
    authService.login(new LoginCommand("expired@example.com", "password123!"));

    Member member = memberRepository.findByEmail("expired@example.com").orElseThrow();

    String expiredToken =
        jwtTokenProvider.createExpiredRefreshToken(
            new kr.lastdish.member.member.domain.MemberId(member.getId()), member.getRole());

    // when & then
    assertThatThrownBy(() -> authService.refresh(new RefreshTokenCommand(expiredToken)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("유효하지 않은 Refresh Token입니다.");
  }

  @Test
  @DisplayName("로그아웃을 요청하면 리프레시 토큰이 삭제되어 재발급 요청 시 예외가 발생한다.")
  void logoutSuccess() {
    // given
    SignUpCommand signUpCommand =
        new SignUpCommand(
            "logoutuser",
            "password123!",
            "로그아웃테스터",
            "010-7777-8888",
            "logout@example.com",
            "MEMBER");
    authService.signUp(signUpCommand);
    TokenResult tokenResult =
        authService.login(new LoginCommand("logout@example.com", "password123!"));

    // when
    authService.logout(new RefreshTokenCommand(tokenResult.refreshToken()));

    // then
    assertThatThrownBy(() -> authService.refresh(new RefreshTokenCommand(tokenResult.refreshToken())))
        .isInstanceOf(BusinessException.class);
  }
}
