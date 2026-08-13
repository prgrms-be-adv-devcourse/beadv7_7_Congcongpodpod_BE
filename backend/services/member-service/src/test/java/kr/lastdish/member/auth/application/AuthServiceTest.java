package kr.lastdish.member.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.member.auth.application.dto.LoginCommand;
import kr.lastdish.member.auth.application.dto.RefreshTokenCommand;
import kr.lastdish.member.auth.application.dto.TokenResult;
import kr.lastdish.member.auth.domain.RefreshToken;
import kr.lastdish.member.auth.domain.RefreshTokenRepository;
import kr.lastdish.member.auth.domain.TokenProvider;
import kr.lastdish.member.auth.exception.AuthErrorCode;
import kr.lastdish.member.member.domain.Member;
import kr.lastdish.member.member.domain.MemberId;
import kr.lastdish.member.member.domain.MemberRepository;
import kr.lastdish.member.member.domain.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @InjectMocks private AuthService authService;

  @Mock private MemberRepository memberRepository;
  @Mock private RefreshTokenRepository refreshTokenRepository;
  @Mock private TokenProvider tokenProvider;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private RedisTemplate<String, String> redisTemplate;
  @Mock private ValueOperations<String, String> valueOperations;

  private String encryptSha256(String text) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
      StringBuilder hexString = new StringBuilder();
      for (byte b : hash) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) hexString.append('0');
        hexString.append(hex);
      }
      return hexString.toString();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  @DisplayName("로그인 시 리프레시 토큰이 저장되고 TokenResult를 반환한다.")
  void loginSavesHashedRefreshToken() {
    // given
    LoginCommand loginCommand = new LoginCommand("hash@example.com", "password123!");
    Member member =
        Member.builder()
            .userName("hashTestUser")
            .password("encodedPassword")
            .name("해시테스터")
            .phone("010-9999-8888")
            .email("hash@example.com")
            .role(Role.MEMBER)
            .build();
    ReflectionTestUtils.setField(member, "id", 1L);

    given(memberRepository.findByEmail("hash@example.com")).willReturn(Optional.of(member));
    given(passwordEncoder.matches("password123!", "encodedPassword")).willReturn(true);
    given(tokenProvider.createAccessToken(any(MemberId.class), any(Role.class)))
        .willReturn("access-token");
    given(tokenProvider.createRefreshToken(any(MemberId.class), any(Role.class)))
        .willReturn("raw-refresh-token");

    // when
    TokenResult tokenResult = authService.login(loginCommand);

    // then
    assertThat(tokenResult.accessToken()).isEqualTo("access-token");
    assertThat(tokenResult.refreshToken()).isEqualTo("raw-refresh-token");
    verify(refreshTokenRepository).save(any(RefreshToken.class));
  }

  @Test
  @DisplayName("유효한 리프레시 토큰으로 재발급을 요청하면 새로운 토큰이 발급된다.")
  void reissueSuccess() {
    // given
    String refreshToken = "valid-refresh-token";
    String hashedRefreshToken = encryptSha256(refreshToken);

    RefreshToken savedToken =
        RefreshToken.builder()
            .email("reissue@example.com")
            .token(hashedRefreshToken)
            .expiryDate(LocalDateTime.now().plusDays(7))
            .build();

    Member member = Member.builder().email("reissue@example.com").role(Role.MEMBER).build();
    MemberId memberId = new MemberId(1L);
    ReflectionTestUtils.setField(member, "id", 1L);

    given(tokenProvider.validateToken(refreshToken)).willReturn(true);
    given(tokenProvider.isRefreshToken(refreshToken)).willReturn(true);
    given(tokenProvider.getMemberId(refreshToken)).willReturn(memberId);
    given(memberRepository.findById(1L)).willReturn(Optional.of(member));
    given(refreshTokenRepository.findByEmail("reissue@example.com"))
        .willReturn(Optional.of(savedToken));
    given(tokenProvider.createAccessToken(any(MemberId.class), any(Role.class)))
        .willReturn("new-access-token");
    given(tokenProvider.createRefreshToken(any(MemberId.class), any(Role.class)))
        .willReturn("new-refresh-token");

    // when
    TokenResult newTokens = authService.refresh(new RefreshTokenCommand(refreshToken));

    // then
    assertThat(newTokens.accessToken()).isEqualTo("new-access-token");
    assertThat(newTokens.refreshToken()).isEqualTo("new-refresh-token");
  }

  @Test
  @DisplayName("위조되거나 변조된 리프레시 토큰으로 재발급을 요청하면 예외가 발생한다.")
  void reissueFailWithInvalidToken() {
    // given
    String invalidRefreshToken = "invalid.token.string";
    given(tokenProvider.validateToken(invalidRefreshToken)).willReturn(false);

    // when & then
    assertThatThrownBy(() -> authService.refresh(new RefreshTokenCommand(invalidRefreshToken)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("유효하지 않은 Refresh Token입니다.");
  }

  @Test
  @DisplayName("로그아웃을 요청하면 리프레시 토큰 삭제 및 Redis 블랙리스트에 등록된다.")
  void logoutSuccess() {
    // given
    String accessToken = "valid-access-token";
    String refreshToken = "valid-refresh-token";
    String hashedRefreshToken = encryptSha256(refreshToken);

    RefreshToken savedToken =
        RefreshToken.builder()
            .email("logout@example.com")
            .token(hashedRefreshToken)
            .expiryDate(LocalDateTime.now().plusDays(7))
            .build();

    given(tokenProvider.validateToken(refreshToken)).willReturn(true);
    given(refreshTokenRepository.findByToken(hashedRefreshToken))
        .willReturn(Optional.of(savedToken));
    given(tokenProvider.validateToken(accessToken)).willReturn(true);
    given(tokenProvider.getExpiration(accessToken)).willReturn(10000L);
    given(redisTemplate.opsForValue()).willReturn(valueOperations);

    // when
    authService.logout(accessToken, new RefreshTokenCommand(refreshToken));

    // then
    verify(valueOperations)
        .set(eq(accessToken), eq("blacklisted"), eq(10000L), eq(TimeUnit.MILLISECONDS));
    verify(refreshTokenRepository).delete(savedToken);
  }

  @Test
  @DisplayName("회원 탈퇴 성공")
  void withdrawSuccess() {
    // given
    Long memberId = 1L;
    Member member = Member.builder().userName("withdrawUser").email("withdraw@example.com").build();
    ReflectionTestUtils.setField(member, "id", memberId);

    given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

    // when
    authService.withdraw("access-token", memberId);

    // then
    assertThat(member.getIsDeleted()).isTrue();
    assertThat(member.getDeletedAt()).isNotNull();
  }

  @Test
  @DisplayName("회원 탈퇴 실패 - 존재하지 않는 회원")
  void withdrawFailWithNotFoundMember() {
    // given
    Long nonExistentMemberId = 99999L;
    given(memberRepository.findById(nonExistentMemberId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> authService.withdraw(null, nonExistentMemberId))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.MEMBER_NOT_FOUND);
  }
}
