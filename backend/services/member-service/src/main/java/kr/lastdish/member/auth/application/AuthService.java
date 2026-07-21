package kr.lastdish.member.auth.application;

import java.time.LocalDateTime;
import kr.lastdish.member.auth.domain.RefreshToken;
import kr.lastdish.member.auth.domain.RefreshTokenRepository;
import kr.lastdish.member.auth.infrastructure.JwtTokenProvider;
import kr.lastdish.member.auth.presentation.dto.LoginRequest;
import kr.lastdish.member.auth.presentation.dto.SignUpRequest;
import kr.lastdish.member.auth.presentation.dto.SignUpResponse;
import kr.lastdish.member.auth.presentation.dto.TokenResponse;
import kr.lastdish.member.member.domain.Member;
import kr.lastdish.member.member.domain.MemberRepository;
import kr.lastdish.member.member.domain.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

  private final MemberRepository memberRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final JwtTokenProvider jwtTokenProvider;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  public SignUpResponse signUp(SignUpRequest request) {
    if (memberRepository.existsByUserName(request.userName())) {
      throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
    }

    if (memberRepository.existsByEmail(request.email())) {
      throw new IllegalArgumentException("이미 등록된 이메일입니다.");
    }

    String encodedPassword = passwordEncoder.encode(request.password());
    Role role = Role.valueOf(request.role().toUpperCase());

    Member member =
        Member.builder()
            .userName(request.userName())
            .password(encodedPassword)
            .name(request.name())
            .phone(request.phone())
            .email(request.email())
            .role(role)
            .build();

    Member savedMember = memberRepository.save(member);

    return SignUpResponse.of(
        savedMember.getId(), savedMember.getUserName(), savedMember.getEmail());
  }

  @Transactional
  public TokenResponse login(LoginRequest request) {
    // 1. 이메일로 회원 조회
    Member member =
        memberRepository
            .findByEmail(request.getEmail())
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이메일입니다."));

    // 2. 비밀번호 검증
    if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
      throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
    }

    // 3. 토큰 생성
    String accessToken = jwtTokenProvider.createAccessToken(member.getEmail());
    String refreshTokenValue = jwtTokenProvider.createRefreshToken(member.getEmail());

    // 4. [보안 조치] DB 저장 시 Refresh Token 암호화
    String encodedRefreshToken = passwordEncoder.encode(refreshTokenValue);

    // 5. Refresh Token 저장 또는 갱신
    LocalDateTime expiryDate = LocalDateTime.now().plusDays(14); // 14일 뒤 만료
    RefreshToken refreshToken =
        refreshTokenRepository
            .findByEmail(member.getEmail())
            .orElse(
                RefreshToken.builder()
                    .email(member.getEmail())
                    .token(encodedRefreshToken)
                    .expiryDate(expiryDate)
                    .build());

    refreshToken.updateToken(encodedRefreshToken, expiryDate);
    refreshTokenRepository.save(refreshToken);

    // 클라이언트에게는 원본 토큰을 전달
    return new TokenResponse(accessToken, refreshTokenValue);
  }

  @Transactional
  public TokenResponse reissue(String requestRefreshToken) {
    // 1. Refresh Token 유효성 검증
    if (!jwtTokenProvider.validateToken(requestRefreshToken)) {
      throw new IllegalArgumentException("유효하지 않은 Refresh Token입니다.");
    }

    // 2. DB에 저장된 Refresh Token인지 확인 (암호화되어 있으므로 매치 확인 혹은 별도 조회 로직 적용)
    // ※ 주의: 만약 DB에 암호화되어 저장되어 있다면, 전달받은 plain 토큰과 비교하기 위해
    // 리포지토리에서 이메일 등으로 조회한 뒤 passwordEncoder.matches()를 통해 검증하거나 구조에 맞게 처리해야 합니다.
    RefreshToken refreshToken =
        refreshTokenRepository
            .findByEmail(jwtTokenProvider.getEmail(requestRefreshToken))
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 Refresh Token입니다."));

    if (!passwordEncoder.matches(requestRefreshToken, refreshToken.getToken())) {
      throw new IllegalArgumentException("Refresh Token 정보가 일치하지 않습니다.");
    }

    // 3. 새로운 Access Token 발급
    String email = refreshToken.getEmail();
    String newAccessToken = jwtTokenProvider.createAccessToken(email);

    return new TokenResponse(newAccessToken, requestRefreshToken);
  }
}
