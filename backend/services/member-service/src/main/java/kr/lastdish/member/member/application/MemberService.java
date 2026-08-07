package kr.lastdish.member.member.application;

import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.member.auth.domain.RefreshTokenRepository;
import kr.lastdish.member.member.application.dto.MemberProfileResult;
import kr.lastdish.member.member.domain.Member;
import kr.lastdish.member.member.domain.MemberRepository;
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
  private final RefreshTokenRepository refreshTokenRepository;
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
    Member member =
        memberRepository
            .findActiveById(memberId)
            .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));

    if (!member.getUserName().equals(requestDto.getUserName())) {
      if (memberRepository.existsByUserName(requestDto.getUserName())) {
        throw new BusinessException(MemberErrorCode.DUPLICATE_USERNAME);
      }
    }

    if (!member.getEmail().equals(requestDto.getEmail())) {
      if (memberRepository.existsByEmail(requestDto.getEmail())) {
        throw new BusinessException(MemberErrorCode.DUPLICATE_EMAIL);
      }
    }

    String encodedPassword = passwordEncoder.encode(requestDto.getPassword());

    member.updateMember(
        requestDto.getUserName(),
        encodedPassword,
        requestDto.getName(),
        requestDto.getPhone(),
        requestDto.getEmail());
  }

  // 회원 탈퇴
  @Transactional
  public void withdrawMember(Long memberId) {
    // 1. 탈퇴 여부와 상관없이 ID로 회원 조회
    Member member =
        memberRepository
            .findById(memberId)
            .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));

    // 2. 이미 탈퇴한 회원인지 체크
    if (Boolean.TRUE.equals(member.getIsDeleted())) {
      throw new BusinessException(MemberErrorCode.ALREADY_WITHDRAWN_MEMBER);
    }

    member.withdraw();

    refreshTokenRepository.deleteByEmail(member.getEmail());
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
