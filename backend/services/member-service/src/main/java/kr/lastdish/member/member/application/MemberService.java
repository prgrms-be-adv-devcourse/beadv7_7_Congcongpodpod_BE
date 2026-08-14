package kr.lastdish.member.member.application;

import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.member.member.application.dto.MemberProfileResult;
import kr.lastdish.member.member.domain.Member;
import kr.lastdish.member.member.domain.MemberRepository;
import kr.lastdish.member.member.domain.SocialProvider;
import kr.lastdish.member.member.exception.MemberErrorCode;
import kr.lastdish.member.member.presentation.dto.MemberUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

  private final MemberRepository memberRepository;
  private final PasswordEncoder passwordEncoder;

  public MemberProfileResult getMemberById(Long memberId) {
    Member member =
        memberRepository
            .findActiveById(memberId)
            .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));

    return MemberProfileResult.from(member);
  }

  @Transactional
  public void updateMember(Long memberId, MemberUpdateRequest requestDto) {

    // 1. 활성화된 회원(탈퇴 제외) 조회
    Member member =
        memberRepository
            .findActiveById(memberId)
            .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));

    // 2. 아이디 변경 시 중복 검사 수행(값이 들어왔을 때만)
    if (requestDto.getUserName() != null
        && !member.getUserName().equals(requestDto.getUserName())) {
      if (memberRepository.existsByUserName(requestDto.getUserName())) {
        throw new BusinessException(MemberErrorCode.DUPLICATE_USERNAME);
      }
    }

    // 3. 이메일 변경 시 중복 검사 수행(값이 들어왔을 때만)
    if (requestDto.getEmail() != null && !member.getEmail().equals(requestDto.getEmail())) {
      if (memberRepository.existsByEmail(requestDto.getEmail())) {
        throw new BusinessException(MemberErrorCode.DUPLICATE_EMAIL);
      }
    }

    // 4. 비밀번호 변경 (값이 들어왔을 때만)
    String encodedPassword = null;
    if (requestDto.getPassword() != null && !requestDto.getPassword().isBlank()) {
      // 소셜 로그인 회원은 비밀번호 변경 불가
      if (member.getProvider() != null && member.getProvider() != SocialProvider.LOCAL) {
        throw new BusinessException(MemberErrorCode.SOCIAL_MEMBER_CANNOT_CHANGE_PASSWORD);
      }
      encodedPassword = passwordEncoder.encode(requestDto.getPassword());
    }

    // 5. 회원 정보 업데이트(null 체크를 통해 기존 값 유지 보장)
    member.updateMember(
        requestDto.getUserName() != null ? requestDto.getUserName() : member.getUserName(),
        encodedPassword != null ? encodedPassword : member.getPassword(),
        requestDto.getName() != null ? requestDto.getName() : member.getName(),
        requestDto.getPhone() != null ? requestDto.getPhone() : member.getPhone(),
        requestDto.getEmail() != null ? requestDto.getEmail() : member.getEmail());
  }

  // 회원 승급
  @Transactional
  public void grantSellerRole(Long memberId) {
    Member member =
        memberRepository
            .findActiveById(memberId)
            .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));

    member.grantSellerRole();
  }
}
