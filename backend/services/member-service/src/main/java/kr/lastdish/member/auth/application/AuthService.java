package kr.lastdish.member.auth.application;

import kr.lastdish.member.auth.infrastructure.TokenProvider;
import kr.lastdish.member.auth.presentation.dto.LoginRequest;
import kr.lastdish.member.auth.presentation.dto.SignUpRequest;
import kr.lastdish.member.auth.presentation.dto.SignUpResponse;
import kr.lastdish.member.auth.presentation.dto.TokenResponse;
import kr.lastdish.member.member.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final MemberRepository memberRepository;
  private final PasswordEncoder passwordEncoder;
  private final RefreshTokenRepository refreshTokenRepository;
  private final TokenProvider tokenProvider;

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
    // 1. 회원 조회
    Member member =
        memberRepository
            .findByEmail(request.email())
            .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 일치하지 않습니다."));

    // 2. 비밀번호 검증
    if (!passwordEncoder.matches(request.password(), member.getPassword())) {
      throw new IllegalArgumentException("이메일 또는 비밀번호가 일치하지 않습니다.");
    }

    // 3. 토큰 생성
    String accessToken = tokenProvider.createAccessToken(member.getId(), member.getRole());
    String refreshTokenVal = tokenProvider.createRefreshToken();

    // 4. 리프레시 토큰 저장
    refreshTokenRepository.save(new RefreshToken(refreshTokenVal, member.getId()));

    return new TokenResponse(accessToken, refreshTokenVal);
  }
}
