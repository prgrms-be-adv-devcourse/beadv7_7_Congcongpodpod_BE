package kr.lastdish.member.auth.application;

import kr.lastdish.member.auth.presentation.dto.SignUpRequest;
import kr.lastdish.member.auth.presentation.dto.SignUpResponse;
import kr.lastdish.member.member.domain.Member;
import kr.lastdish.member.member.domain.MemberRepository;
import kr.lastdish.member.member.domain.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final MemberRepository memberRepository;
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
}
