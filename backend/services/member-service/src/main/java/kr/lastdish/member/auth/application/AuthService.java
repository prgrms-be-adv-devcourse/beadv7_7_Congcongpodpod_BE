package kr.lastdish.member.auth.application;

import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.member.auth.application.dto.*;
import kr.lastdish.member.auth.domain.RefreshToken;
import kr.lastdish.member.auth.domain.RefreshTokenRepository;
import kr.lastdish.member.auth.domain.TokenProvider;
import kr.lastdish.member.member.domain.Member;
import kr.lastdish.member.member.domain.MemberId;
import kr.lastdish.member.member.domain.MemberRepository;
import kr.lastdish.member.member.domain.Role;
import kr.lastdish.member.member.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

  private final MemberRepository memberRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final TokenProvider tokenProvider;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  public SignUpResult signUp(SignUpCommand command) {
    if (memberRepository.existsByUserName(command.userName())) {
      throw new BusinessException(ErrorCode.DUPLICATE_USERNAME);
    }

    if (memberRepository.existsByEmail(command.email())) {
      throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
    }

    String encodedPassword = passwordEncoder.encode(command.password());
    Role role = Role.from(command.role().toUpperCase());

    Member member =
        Member.builder()
            .userName(command.userName())
            .password(encodedPassword)
            .name(command.name())
            .phone(command.phone())
            .email(command.email())
            .role(role)
            .build();

    Member savedMember = memberRepository.save(member);

    return new SignUpResult(
        savedMember.getId(),
        savedMember.getUserName(),
        savedMember.getEmail());
  }

  @Transactional
  public TokenResult login(LoginCommand command) {
    // 1. 이메일로 회원 조회 (이메일 없음 -> 404)
    Member member =
        memberRepository
            .findByEmail(command.email())
            .orElseThrow(() -> new BusinessException(ErrorCode.EMAIL_NOT_FOUND));

    // 2. 비밀번호 검증 (비밀번호 불일치 -> 401)
    if (!passwordEncoder.matches(command.password(), member.getPassword())) {
      throw new BusinessException(ErrorCode.INVALID_PASSWORD);
    }

    // 3. 토큰 생성
    MemberId memberId = new MemberId(member.getId());
    Role role = member.getRole();

    String accessToken = tokenProvider.createAccessToken(memberId, role);
    String refreshTokenValue = tokenProvider.createRefreshToken(memberId, role);

    // 4. Refresh Token을 SHA-256 해시로 변환
    String hashedRefreshToken = encryptSha256(refreshTokenValue);

    // 5. Refresh Token 저장 또는 갱신
    LocalDateTime expiryDate = LocalDateTime.now().plusDays(14);
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
  public void logout(RefreshTokenCommand command) {
    String refreshToken = command.refreshToken();

    // 1. 토큰 유효성 검증
    if (!tokenProvider.validateToken(refreshToken)) {
      throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    // 2. 요청받은 토큰을 해시화하여 DB에 저장된 해시값과 일치하는 토큰 조회
    String hashedRefreshToken = encryptSha256(refreshToken);
    RefreshToken savedToken =
        refreshTokenRepository
            .findByToken(hashedRefreshToken)
            .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

    // 3. 토큰 삭제
    refreshTokenRepository.delete(savedToken);
  }

  @Transactional
  public TokenResult refresh(RefreshTokenCommand command) {
    String requestRefreshToken = command.refreshToken();

    // 1. Refresh Token 유효성 검증
    if (!tokenProvider.validateToken(requestRefreshToken)) {
      throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
    }
    //  토큰 타입이 실제로 refresh인지 검증 (Access Token으로 재발급 요청 차단)
    if (!tokenProvider.isRefreshToken(requestRefreshToken)) {
      throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    // 2. 토큰에서 MemberId 추출 후 회원 조회
    MemberId memberId = tokenProvider.getMemberId(requestRefreshToken);

    Member member =
        memberRepository
            .findById(memberId.getValue())
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

    // 3. 이메일로 DB에 저장된 Refresh Token 조회
    RefreshToken refreshToken =
        refreshTokenRepository
            .findByEmail(member.getEmail())
            .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

    // 4. 요청받은 토큰을 해시화하여 DB에 저장된 해시값과 비교
    String hashedRequestToken = encryptSha256(requestRefreshToken);
    if (!hashedRequestToken.equals(refreshToken.getToken())) {
      throw new BusinessException(ErrorCode.REFRESH_TOKEN_MISMATCH);
    }

    // 5. 새로운 Access Token 및 Refresh Token 발급
    String newAccessToken = tokenProvider.createAccessToken(memberId, member.getRole());
    String newRefreshTokenValue = tokenProvider.createRefreshToken(memberId, member.getRole());

    // 6. 새로운 Refresh Token을 해시화하여 DB 갱신
    String hashedNewRefreshToken = encryptSha256(newRefreshTokenValue);
    LocalDateTime expiryDate = LocalDateTime.now().plusDays(14);

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
}
