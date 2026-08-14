package kr.lastdish.member.auth.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.member.auth.application.dto.*;
import kr.lastdish.member.auth.domain.RefreshToken;
import kr.lastdish.member.auth.domain.RefreshTokenRepository;
import kr.lastdish.member.auth.domain.TokenProvider;
import kr.lastdish.member.auth.exception.AuthErrorCode;
import kr.lastdish.member.auth.infrastructure.client.KakaoOAuthClient;
import kr.lastdish.member.member.domain.*;
import kr.lastdish.member.member.exception.MemberErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AuthService {

  private final MemberRepository memberRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final TokenProvider tokenProvider;
  private final PasswordEncoder passwordEncoder;
  private final RedisTemplate<String, String> redisTemplate;
  private final KakaoOAuthClient kakaoOAuthClient;

  @Transactional
  public SignUpResult signUp(SignUpCommand command) {
    if (memberRepository.existsByUserName(command.userName())) {
      throw new BusinessException(AuthErrorCode.DUPLICATE_USERNAME);
    }

    if (memberRepository.existsByEmail(command.email())) {
      throw new BusinessException(AuthErrorCode.DUPLICATE_EMAIL);
    }

    String encodedPassword = passwordEncoder.encode(command.password());

    Member member =
        Member.builder()
            .userName(command.userName())
            .password(encodedPassword)
            .name(command.name())
            .phone(command.phone())
            .email(command.email())
            .role(Role.MEMBER)
            .build();

    Member savedMember = memberRepository.save(member);

    return new SignUpResult(savedMember.getId(), savedMember.getUserName(), savedMember.getEmail());
  }

  @Transactional
  public TokenResult login(LoginCommand command) {
    // 1. 이메일로 회원 조회 (이메일 없음 -> 404)
    Member member =
        memberRepository
            .findByEmail(command.email())
            .orElseThrow(() -> new BusinessException(AuthErrorCode.EMAIL_NOT_FOUND));

    // 소셜 가입 회원이 일반 로그인을 시도할 경우 차단
    if (member.getProvider() != null && member.getProvider() != SocialProvider.LOCAL) {
      throw new BusinessException(AuthErrorCode.SOCIAL_MEMBER_LOGIN_RESTRICTED);
    }

    // 2. 비밀번호 검증 (비밀번호 불일치 -> 401)
    if (!passwordEncoder.matches(command.password(), member.getPassword())) {
      throw new BusinessException(AuthErrorCode.INVALID_PASSWORD);
    }

    // 3. 토큰 생성
    MemberId memberId = new MemberId(member.getId());
    Role role = member.getRole();

    String accessToken = tokenProvider.createAccessToken(memberId, role);
    String refreshTokenValue = tokenProvider.createRefreshToken(memberId, role);

    // 4. Refresh Token을 SHA-256 해시로 변환
    String hashedRefreshToken = encryptSha256(refreshTokenValue);

    // 5. Refresh Token 저장 또는 갱신
    LocalDateTime expiryDate = LocalDateTime.now().plusDays(28);
    RefreshToken refreshToken =
        refreshTokenRepository
            .findByEmail(member.getEmail())
            .orElse(
                RefreshToken.builder()
                    .email(member.getEmail())
                    .token(hashedRefreshToken)
                    .expiryDate(expiryDate)
                    .build());

    refreshToken.updateToken(hashedRefreshToken, expiryDate);
    refreshTokenRepository.save(refreshToken);

    return new TokenResult(accessToken, refreshTokenValue);
  }

  @Transactional
  public TokenResult kakaoLogin(String code) {
    // 1. 인가 코드로 카카오 Access Token 발급 및 유저 정보 조회
    KakaoUserInfoResponse userInfo = kakaoOAuthClient.getKakaoUserInfo(code);

    String socialId = String.valueOf(userInfo.id());
    String email =
        (userInfo.email() != null && !userInfo.email().isBlank())
            ? userInfo.email()
            : "kakao_" + socialId + "@kakao.user";
    String name = userInfo.nickname();

    // 2. 이메일 혹은 소셜 ID로 기존 회원 조회 (없으면 자동 회원가입)
    Member member =
        memberRepository
            .findByProviderAndProviderId(SocialProvider.KAKAO, socialId)
            .orElseGet(
                () ->
                    // 없으면 이메일로 기존 회원 조회
                    memberRepository
                        .findByEmail(email)
                        .map(
                            existingMember -> {
                              // 기존 일반 회원(LOCAL)이 있으면 카카오 계정 정보 연동/업데이트
                              existingMember.linkSocialAccount(SocialProvider.KAKAO, socialId);
                              return existingMember;
                            })
                        .orElseGet(
                            () -> {
                              // 소셜 회원은 패스워드가 없으므로 임의의 난수 인코딩 값 혹은 null 방지 처리
                              String encodedRandomPassword =
                                  passwordEncoder.encode(java.util.UUID.randomUUID().toString());

                              Member newMember =
                                  Member.builder()
                                      .userName("kakao_" + socialId)
                                      .password(encodedRandomPassword)
                                      .name(name != null ? name : "카카오사용자")
                                      .phone("010-0000-0000")
                                      .email(email)
                                      .role(Role.MEMBER)
                                      .provider(SocialProvider.KAKAO)
                                      .providerId(socialId)
                                      .build();
                              return memberRepository.save(newMember);
                            }));

    // 3. 서비스 자체 JWT 토큰 생성
    MemberId memberId = new MemberId(member.getId());
    Role role = member.getRole();

    String accessToken = tokenProvider.createAccessToken(memberId, role);
    String refreshTokenValue = tokenProvider.createRefreshToken(memberId, role);

    // 4. Refresh Token 해시화 및 DB 저장/갱신
    String hashedRefreshToken = encryptSha256(refreshTokenValue);
    LocalDateTime expiryDate = LocalDateTime.now().plusDays(28);

    RefreshToken refreshToken =
        refreshTokenRepository
            .findByEmail(member.getEmail())
            .orElse(
                RefreshToken.builder()
                    .email(member.getEmail())
                    .token(hashedRefreshToken)
                    .expiryDate(expiryDate)
                    .build());

    refreshToken.updateToken(hashedRefreshToken, expiryDate);
    refreshTokenRepository.save(refreshToken);

    return new TokenResult(accessToken, refreshTokenValue);
  }

  @Transactional
  public void logout(String accessToken, RefreshTokenCommand command) {
    String refreshToken = command.refreshToken();

    // 1. Refresh Token 유효성 검증
    if (!tokenProvider.validateToken(refreshToken)) {
      throw new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
    }

    // 2. 요청받은 토큰을 해시화하여 DB에 저장된 해시값과 일치하는 토큰 조회
    String hashedRefreshToken = encryptSha256(refreshToken);
    RefreshToken savedToken =
        refreshTokenRepository
            .findByToken(hashedRefreshToken)
            .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN));

    // 3. 통합 무효화 로직 사용 (Access Token 블랙리스트 + DB Refresh Token 삭제)
    invalidateTokens(accessToken, savedToken.getEmail());
  }

  @Transactional
  public void withdraw(String accessToken, Long memberId) {

    // 1. 회원 조회
    if (memberId == null) {
      throw new IllegalArgumentException("회원 ID가 존재하지 않습니다.");
    }

    Member member =
        memberRepository
            .findById(memberId)
            .orElseThrow(() -> new BusinessException(AuthErrorCode.MEMBER_NOT_FOUND));

    // 2. 이미 탈퇴한 회원인지 체크
    if (Boolean.TRUE.equals(member.getIsDeleted())) {
      throw new BusinessException(AuthErrorCode.ALREADY_WITHDRAWN_MEMBER);
    }

    // 3. 카카오 소셜 로그인 유저일 경우 카카오 측 연결 끊기
    if (member.getProvider() == SocialProvider.KAKAO && member.getProviderId() != null) {
      try {
        kakaoOAuthClient.unlink(member.getProviderId());
      } catch (Exception e) {
        log.error("카카오 연결 해제 중 오류 발생 (회원 탈퇴는 계속 진행됩니다): {}", e.getMessage());
      }
    }

    // 4. 토큰 무효화
    invalidateTokens(accessToken, member.getEmail());

    // 5. 회원 탈퇴 처리
    member.withdraw();
  }

  @Transactional
  public TokenResult refresh(RefreshTokenCommand command) {
    String requestRefreshToken = command.refreshToken();

    // 1. Refresh Token 유효성 검증
    if (!tokenProvider.validateToken(requestRefreshToken)) {
      throw new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
    }
    //  토큰 타입이 실제로 refresh인지 검증 (Access Token으로 재발급 요청 차단)
    if (!tokenProvider.isRefreshToken(requestRefreshToken)) {
      throw new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
    }

    // 2. 토큰에서 MemberId 추출 후 회원 조회
    MemberId memberId = tokenProvider.getMemberId(requestRefreshToken);

    Member member =
        memberRepository
            .findById(memberId.getValue())
            .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));

    // 3. 이메일로 DB에 저장된 Refresh Token 조회
    RefreshToken refreshToken =
        refreshTokenRepository
            .findByEmail(member.getEmail())
            .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN));

    // 4. 요청받은 토큰을 해시화하여 DB에 저장된 해시값과 비교
    String hashedRequestToken = encryptSha256(requestRefreshToken);
    if (!hashedRequestToken.equals(refreshToken.getToken())) {
      throw new BusinessException(AuthErrorCode.REFRESH_TOKEN_MISMATCH);
    }

    // 5. 새로운 Access Token 및 Refresh Token 발급
    String newAccessToken = tokenProvider.createAccessToken(memberId, member.getRole());
    String newRefreshTokenValue = tokenProvider.createRefreshToken(memberId, member.getRole());

    // 6. 새로운 Refresh Token을 해시화하여 DB 갱신
    String hashedNewRefreshToken = encryptSha256(newRefreshTokenValue);
    LocalDateTime expiryDate = LocalDateTime.now().plusDays(28);

    refreshToken.updateToken(hashedNewRefreshToken, expiryDate);
    refreshTokenRepository.save(refreshToken);

    return new TokenResult(newAccessToken, newRefreshTokenValue);
  }

  private String encryptSha256(String text) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
      StringBuilder hexString = new StringBuilder();
      for (byte b : hash) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) {
          hexString.append('0');
        }
        hexString.append(hex);
      }
      return hexString.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 암호화 실패", e);
    }
  }

  private void invalidateTokens(String accessToken, String email) {
    // 1. Access Token 블랙리스트 처리 (Redis)
    if (accessToken != null && tokenProvider.validateToken(accessToken)) {
      long expiration = tokenProvider.getExpiration(accessToken);
      if (expiration > 0) {
        try {
          redisTemplate
              .opsForValue()
              .set(
                  accessToken,
                  "blacklisted",
                  expiration,
                  java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (Exception e) {
          // Redis가 다운되거나 네트워크가 끊겼을 경우 사용자가 로그아웃이나 회원 탈퇴를 할 수 없게 되기 때문에 log.error 사용
          log.error("Failed to add access token to Redis blacklist: {}", e.getMessage());
        }
      }
    }

    // 2. DB에서 Refresh Token 삭제
    refreshTokenRepository.deleteByEmail(email);
  }
}
