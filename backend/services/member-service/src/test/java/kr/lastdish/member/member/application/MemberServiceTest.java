package kr.lastdish.member.member.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.member.auth.domain.RefreshTokenRepository;
import kr.lastdish.member.member.domain.Member;
import kr.lastdish.member.member.domain.MemberRepository;
import kr.lastdish.member.member.domain.Role;
import kr.lastdish.member.member.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

  @Mock private MemberRepository memberRepository;
  @Mock private RefreshTokenRepository refreshTokenRepository;

  @InjectMocks private MemberService memberService;

  @Test
  @DisplayName("회원 탈퇴 성공")
  void withdrawMember_success() {
    // given
    Long memberId = 1L;
    Member member =
        Member.builder()
            .userName("testuser")
            .password("encodedPassword")
            .name("테스터")
            .phone("010-1234-5678")
            .email("test@test.com")
            .role(Role.MEMBER)
            .build();

    given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

    // when
    memberService.withdrawMember(memberId);

    // then
    assertThat(member.getIsDeleted()).isTrue();
    assertThat(member.getDeletedAt()).isNotNull();
    verify(memberRepository).findById(memberId);
    verify(refreshTokenRepository).deleteByEmail(member.getEmail());
  }

  @Test
  @DisplayName("회원 탈퇴 실패 - 존재하지 않는 회원")
  void withdrawMember_notFound() {
    // given
    Long memberId = 999L;
    given(memberRepository.findById(anyLong())).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> memberService.withdrawMember(memberId))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
  }
}
