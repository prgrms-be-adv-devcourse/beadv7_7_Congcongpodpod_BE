package kr.lastdish.member.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import kr.lastdish.member.auth.application.dto.KakaoUserInfoResponse;
import kr.lastdish.member.auth.application.dto.TokenResult;
import kr.lastdish.member.auth.domain.RefreshTokenRepository;
import kr.lastdish.member.auth.domain.TokenProvider;
import kr.lastdish.member.auth.infrastructure.client.KakaoOAuthClient;
import kr.lastdish.member.member.domain.Member;
import kr.lastdish.member.member.domain.MemberRepository;
import kr.lastdish.member.member.domain.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceKakaoTest {

  @Mock private MemberRepository memberRepository;
  @Mock private RefreshTokenRepository refreshTokenRepository;
  @Mock private TokenProvider tokenProvider;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private KakaoOAuthClient kakaoOAuthClient;

  @InjectMocks private AuthService authService;

  @Test
  @DisplayName("카카오 로그인 성공 - 기존 회원이 없는 경우 자동 회원가입 후 토큰 발급")
  void kakaoLogin_success_newMember() {
    String code = "sample_kakao_auth_code";
    String email = "kakao@test.com";
    String nickname = "카카오테스터";
    Long kakaoId = 123456789L;

    KakaoUserInfoResponse userInfoResponse =
        new KakaoUserInfoResponse(
            kakaoId,
            new KakaoUserInfoResponse.KakaoAccount(
                email, new KakaoUserInfoResponse.Profile(nickname)));

    given(kakaoOAuthClient.getKakaoUserInfo(code)).willReturn(userInfoResponse);
    given(memberRepository.findByEmail(email)).willReturn(Optional.empty());
    given(refreshTokenRepository.findByEmail(email)).willReturn(Optional.empty());
    given(passwordEncoder.encode(anyString())).willReturn("encoded_password");

    Member savedMember =
        Member.builder()
            .userName("kakao_" + kakaoId)
            .password("encoded_password")
            .name(nickname)
            .phone("010-0000-0000")
            .email(email)
            .role(Role.MEMBER)
            .build();

    ReflectionTestUtils.setField(savedMember, "id", 1L);

    given(memberRepository.save(any(Member.class))).willReturn(savedMember);
    given(tokenProvider.createAccessToken(any(), any())).willReturn("mock_access_token");
    given(tokenProvider.createRefreshToken(any(), any())).willReturn("mock_refresh_token");

    TokenResult result = authService.kakaoLogin(code);

    assertThat(result).isNotNull();
    assertThat(result.accessToken()).isEqualTo("mock_access_token");
    assertThat(result.refreshToken()).isEqualTo("mock_refresh_token");
    verify(memberRepository).save(any(Member.class));
  }

  @Test
  @DisplayName("카카오 로그인 성공 - 이미 가입된 회원이 있는 경우 회원가입 없이 토큰 발급")
  void kakaoLogin_success_existingMember() {
    String code = "sample_kakao_auth_code";
    String email = "kakao@test.com";
    String nickname = "카카오테스터";
    Long kakaoId = 123456789L;

    KakaoUserInfoResponse userInfoResponse =
        new KakaoUserInfoResponse(
            kakaoId,
            new KakaoUserInfoResponse.KakaoAccount(
                email, new KakaoUserInfoResponse.Profile(nickname)));

    Member existingMember =
        Member.builder()
            .userName("kakao_" + kakaoId)
            .password("encoded_password")
            .name(nickname)
            .phone("010-0000-0000")
            .email(email)
            .role(Role.MEMBER)
            .build();

    ReflectionTestUtils.setField(existingMember, "id", 1L);

    given(kakaoOAuthClient.getKakaoUserInfo(code)).willReturn(userInfoResponse);
    given(memberRepository.findByEmail(email)).willReturn(Optional.of(existingMember));
    given(refreshTokenRepository.findByEmail(email)).willReturn(Optional.empty());
    given(tokenProvider.createAccessToken(any(), any())).willReturn("mock_access_token");
    given(tokenProvider.createRefreshToken(any(), any())).willReturn("mock_refresh_token");

    TokenResult result = authService.kakaoLogin(code);

    assertThat(result).isNotNull();
    assertThat(result.accessToken()).isEqualTo("mock_access_token");
    assertThat(result.refreshToken()).isEqualTo("mock_refresh_token");
  }
}
