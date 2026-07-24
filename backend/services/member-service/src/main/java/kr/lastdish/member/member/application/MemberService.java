package kr.lastdish.member.member.application;

import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.member.auth.domain.RefreshTokenRepository;
import kr.lastdish.member.member.domain.Member;
import kr.lastdish.member.member.domain.MemberRepository;
import kr.lastdish.member.member.exception.ErrorCode;
import kr.lastdish.member.member.presentation.dto.MemberProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

  private final MemberRepository memberRepository;
  private final RefreshTokenRepository refreshTokenRepository;

  public MemberProfileResponse getMemberById(Long memberId) {
    Member member =
        memberRepository
            .findById(memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

    return MemberProfileResponse.from(member);
  }

  // 회원 탈퇴
  @Transactional
  public void withdrawMember(Long memberId) {
    // 1. 탈퇴 여부와 상관없이 ID로 회원 조회
    Member member =
        memberRepository
            .findById(memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

    // 2. 이미 탈퇴한 회원인지 체크
    if (Boolean.TRUE.equals(member.getIsDeleted())) {
      throw new BusinessException(ErrorCode.ALREADY_WITHDRAWN_MEMBER);
    }

    member.withdraw();

    refreshTokenRepository.deleteByEmail(member.getEmail());
  }
}
