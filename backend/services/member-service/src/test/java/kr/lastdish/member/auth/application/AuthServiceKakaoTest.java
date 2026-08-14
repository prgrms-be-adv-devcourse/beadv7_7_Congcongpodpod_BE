package kr.lastdish.member.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.member.auth.application.dto.KakaoUserInfoResponse;
import kr.lastdish.member.auth.application.dto.TokenResult;
import kr.lastdish.member.auth.domain.RefreshTokenRepository;
import kr.lastdish.member.auth.domain.TokenProvider;
import kr.lastdish.member.auth.exception.AuthErrorCode;
import kr.lastdish.member.auth.infrastructure.client.KakaoOAuthClient;
import kr.lastdish.member.member.domain.Member;
import kr.lastdish.member.member.domain.MemberRepository;
import kr.lastdish.member.member.domain.Role;
import kr.lastdish.member.member.domain.SocialProvider;
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

  private static final String AUTHORIZATION_CODE = "sample_kakao_auth_code";
  private static final String EMAIL = "kakao@test.com";
  private static final String NICKNAME = "카카오테스터";
  private static final Long KAKAO_ID = 123456789L;
  private static final String PROVIDER_ID = "123456789";

  @Mock private MemberRepository memberRepository;
  @Mock private RefreshTokenRepository refreshTokenRepository;
  @Mock private TokenProvider tokenProvider;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private KakaoOAuthClient kakaoOAuthClient;

  @InjectMocks private AuthService authService;

  @Test
  @DisplayName("카카오 로그인 성공 - 기존 회원이 없으면 자동 가입 후 토큰을 발급한다")
  void kakaoLogin_success_newMember() {
    // given
    KakaoUserInfoResponse kakaoUserInfo = createKakaoUserInfo(EMAIL);

    Member savedMember = createKakaoMember(1L, EMAIL);

    given(kakaoOAuthClient.getKakaoUserInfo(AUTHORIZATION_CODE)).willReturn(kakaoUserInfo);
    given(memberRepository.findByProviderAndProviderId(SocialProvider.KAKAO, PROVIDER_ID))
        .willReturn(Optional.empty());
    given(memberRepository.findByEmail(EMAIL)).willReturn(Optional.empty());
    given(passwordEncoder.encode(anyString())).willReturn("encoded_password");
    given(memberRepository.save(any(Member.class))).willReturn(savedMember);
    given(refreshTokenRepository.findByEmail(EMAIL)).willReturn(Optional.empty());
    given(tokenProvider.createAccessToken(any(), any())).willReturn("mock_access_token");
    given(tokenProvider.createRefreshToken(any(), any())).willReturn("mock_refresh_token");

    // when
    TokenResult result = authService.kakaoLogin(AUTHORIZATION_CODE);

    // then
    assertThat(result).isNotNull();
    assertThat(result.accessToken()).isEqualTo("mock_access_token");
    assertThat(result.refreshToken()).isEqualTo("mock_refresh_token");

    verify(memberRepository).findByProviderAndProviderId(SocialProvider.KAKAO, PROVIDER_ID);
    verify(memberRepository).findByEmail(EMAIL);
    verify(memberRepository).save(any(Member.class));
  }

  @Test
  @DisplayName("카카오 로그인 성공 - 기존 카카오 회원은 provider와 providerId로 조회한다")
  void kakaoLogin_success_existingKakaoMember() {
    // given
    KakaoUserInfoResponse kakaoUserInfo = createKakaoUserInfo(EMAIL);

    Member existingKakaoMember = createKakaoMember(1L, EMAIL);

    given(kakaoOAuthClient.getKakaoUserInfo(AUTHORIZATION_CODE)).willReturn(kakaoUserInfo);
    given(memberRepository.findByProviderAndProviderId(SocialProvider.KAKAO, PROVIDER_ID))
        .willReturn(Optional.of(existingKakaoMember));
    given(refreshTokenRepository.findByEmail(EMAIL)).willReturn(Optional.empty());
    given(tokenProvider.createAccessToken(any(), any())).willReturn("mock_access_token");
    given(tokenProvider.createRefreshToken(any(), any())).willReturn("mock_refresh_token");

    // when
    TokenResult result = authService.kakaoLogin(AUTHORIZATION_CODE);

    // then
    assertThat(result).isNotNull();
    assertThat(result.accessToken()).isEqualTo("mock_access_token");
    assertThat(result.refreshToken()).isEqualTo("mock_refresh_token");

    verify(memberRepository).findByProviderAndProviderId(SocialProvider.KAKAO, PROVIDER_ID);
    verify(memberRepository, never()).findByEmail(anyString());
    verify(memberRepository, never()).save(any(Member.class));
  }

  @Test
  @DisplayName("카카오 로그인 실패 - 동일 이메일의 일반 회원을 자동 연결하지 않는다")
  void kakaoLogin_fail_sameEmailLocalMember() {
    // given
    String localMemberEmail = "member@example.com";

    KakaoUserInfoResponse kakaoUserInfo = createKakaoUserInfo(localMemberEmail);

    Member localMember = createLocalMember(2L, localMemberEmail);

    given(kakaoOAuthClient.getKakaoUserInfo(AUTHORIZATION_CODE)).willReturn(kakaoUserInfo);
    given(memberRepository.findByProviderAndProviderId(SocialProvider.KAKAO, PROVIDER_ID))
        .willReturn(Optional.empty());
    given(memberRepository.findByEmail(localMemberEmail)).willReturn(Optional.of(localMember));

    // when & then
    assertThatThrownBy(() -> authService.kakaoLogin(AUTHORIZATION_CODE))
        .isInstanceOfSatisfying(
            BusinessException.class,
            exception ->
                assertThat(exception.getErrorCode())
                    .isEqualTo(AuthErrorCode.KAKAO_EMAIL_ALREADY_REGISTERED));

    verify(memberRepository).findByProviderAndProviderId(SocialProvider.KAKAO, PROVIDER_ID);
    verify(memberRepository).findByEmail(localMemberEmail);
    verify(memberRepository, never()).save(any(Member.class));
    verify(tokenProvider, never()).createAccessToken(any(), any());
    verify(tokenProvider, never()).createRefreshToken(any(), any());
  }

  private KakaoUserInfoResponse createKakaoUserInfo(String email) {
    return new KakaoUserInfoResponse(
        KAKAO_ID,
        new KakaoUserInfoResponse.KakaoAccount(email, new KakaoUserInfoResponse.Profile(NICKNAME)));
  }

  private Member createKakaoMember(Long memberId, String email) {
    Member member =
        Member.builder()
            .userName("kakao_" + PROVIDER_ID)
            .password("encoded_password")
            .name(NICKNAME)
            .phone("010-0000-0000")
            .email(email)
            .role(Role.MEMBER)
            .provider(SocialProvider.KAKAO)
            .providerId(PROVIDER_ID)
            .build();

    ReflectionTestUtils.setField(member, "id", memberId);
    return member;
  }

  private Member createLocalMember(Long memberId, String email) {
    Member member =
        Member.builder()
            .userName("local_member")
            .password("encoded_password")
            .name("일반회원")
            .phone("010-1111-2222")
            .email(email)
            .role(Role.MEMBER)
            .provider(SocialProvider.LOCAL)
            .build();

    ReflectionTestUtils.setField(member, "id", memberId);
    return member;
  }
}
