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
    // 1. 이메일로 회원 조회 (아이디로 로그인을 지원하신다면 userName으로 변경 가능합니다)
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

    // 4. Refresh Token 저장 또는 갱신
    LocalDateTime expiryDate = LocalDateTime.now().plusDays(14); // 예시: 14일 뒤 만료
    RefreshToken refreshToken =
        refreshTokenRepository
            .findByEmail(member.getEmail())
            .orElse(
                RefreshToken.builder()
                    .email(member.getEmail())
                    .token(refreshTokenValue)
                    .expiryDate(expiryDate)
                    .build());

    refreshToken.updateToken(refreshTokenValue, expiryDate);
    refreshTokenRepository.save(refreshToken);

    return new TokenResponse(accessToken, refreshTokenValue);
  }

  @Transactional
  public TokenResponse reissue(String requestRefreshToken) {
    // 1. Refresh Token 유효성 검증
    if (!jwtTokenProvider.validateToken(requestRefreshToken)) {
      throw new IllegalArgumentException("유효하지 않은 Refresh Token입니다.");
    }

    // 2. DB에 저장된 Refresh Token인지 확인
    RefreshToken refreshToken =
        refreshTokenRepository
            .findByToken(requestRefreshToken)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 Refresh Token입니다."));

    // 3. 새로운 Access Token 발급
    String email = refreshToken.getEmail();
    String newAccessToken = jwtTokenProvider.createAccessToken(email);

    return new TokenResponse(newAccessToken, requestRefreshToken);
  }
}
