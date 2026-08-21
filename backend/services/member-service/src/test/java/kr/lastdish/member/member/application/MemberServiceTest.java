package kr.lastdish.member.member.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.member.member.application.event.MemberEventWriter;
import kr.lastdish.member.member.domain.Member;
import kr.lastdish.member.member.domain.MemberRepository;
import kr.lastdish.member.member.exception.MemberErrorCode;
import kr.lastdish.member.member.presentation.dto.MemberUpdateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

  @Mock private MemberRepository memberRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private MemberEventWriter memberEventWriter;
  @InjectMocks private MemberService memberService;

  @Test
  @DisplayName("회원 정보 수정 성공")
  void updateMember_success() {
    // given
    Long memberId = 1L;
    Member member =
        Member.builder()
            .userName("oldUserName")
            .password("oldPassword")
            .name("oldName")
            .phone("010-1111-2222")
            .email("old@email.com")
            .build();

    ReflectionTestUtils.setField(member, "id", memberId);

    MemberUpdateRequest request = new MemberUpdateRequest();
    ReflectionTestUtils.setField(request, "userName", "newUserName");
    ReflectionTestUtils.setField(request, "password", "newPassword123");
    ReflectionTestUtils.setField(request, "name", "newName");
    ReflectionTestUtils.setField(request, "phone", "010-3333-4444");
    ReflectionTestUtils.setField(request, "email", "new@email.com");

    given(memberRepository.findActiveWithLockById(memberId)).willReturn(Optional.of(member));
    given(memberRepository.existsByUserName("newUserName")).willReturn(false);
    given(memberRepository.existsByEmail("new@email.com")).willReturn(false);
    given(passwordEncoder.encode("newPassword123")).willReturn("encodedNewPassword");

    // when
    memberService.updateMember(memberId, request);

    // then
    assertThat(member.getUserName()).isEqualTo("newUserName");
    assertThat(member.getPassword()).isEqualTo("encodedNewPassword");
    assertThat(member.getName()).isEqualTo("newName");
    assertThat(member.getPhone()).isEqualTo("010-3333-4444");
    assertThat(member.getEmail()).isEqualTo("new@email.com");
    verify(memberEventWriter).appendUpdated(member);
  }

  @Test
  @DisplayName("회원 정보 수정 실패 - 중복된 유저네임")
  void updateMember_fail_duplicate_username() {
    // given
    Long memberId = 1L;
    Member member =
        Member.builder()
            .userName("oldUserName")
            .password("oldPassword")
            .name("oldName")
            .phone("010-1111-2222")
            .email("old@email.com")
            .build();

    MemberUpdateRequest request = new MemberUpdateRequest();
    ReflectionTestUtils.setField(request, "userName", "duplicateUserName");
    ReflectionTestUtils.setField(request, "password", "newPassword123");
    ReflectionTestUtils.setField(request, "name", "newName");
    ReflectionTestUtils.setField(request, "phone", "010-3333-4444");
    ReflectionTestUtils.setField(request, "email", "old@email.com");

    given(memberRepository.findActiveWithLockById(memberId)).willReturn(Optional.of(member));
    given(memberRepository.existsByUserName("duplicateUserName")).willReturn(true);

    // when & then
    assertThatThrownBy(() -> memberService.updateMember(memberId, request))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(MemberErrorCode.DUPLICATE_USERNAME);
  }

  @Test
  @DisplayName("회원 정보 수정 성공 - 비밀번호가 없는 경우 기존 비밀번호 유지 및 암호화 미실행")
  void updateMember_success_without_password() {
    // given
    Long memberId = 1L;
    Member member =
        Member.builder()
            .userName("oldUserName")
            .password("oldEncodedPassword")
            .name("oldName")
            .phone("010-1111-2222")
            .email("old@email.com")
            .build();

    ReflectionTestUtils.setField(member, "id", memberId);

    MemberUpdateRequest request = new MemberUpdateRequest();
    ReflectionTestUtils.setField(request, "userName", "oldUserName");
    ReflectionTestUtils.setField(request, "password", null); // 비밀번호를 null로 전달
    ReflectionTestUtils.setField(request, "name", "newName");
    ReflectionTestUtils.setField(request, "phone", "010-3333-4444");
    ReflectionTestUtils.setField(request, "email", "old@email.com");

    given(memberRepository.findActiveWithLockById(memberId)).willReturn(Optional.of(member));

    // when
    memberService.updateMember(memberId, request);

    // then
    assertThat(member.getName()).isEqualTo("newName");
    assertThat(member.getPassword()).isEqualTo("oldEncodedPassword"); // 기존 비밀번호가 유지되어야 함
    verify(passwordEncoder, org.mockito.Mockito.never())
        .encode(org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  @DisplayName("회원 정보 수정 성공 - 특정 필드(전화번호)만 단독 수정")
  void updateMember_success_partial_update() {
    // given
    Long memberId = 1L;
    Member member =
        Member.builder()
            .userName("oldUserName")
            .password("oldPassword")
            .name("oldName")
            .phone("010-1111-2222")
            .email("old@email.com")
            .build();

    ReflectionTestUtils.setField(member, "id", memberId);

    // 전화번호만 새로 세팅하고, 나머지 필드는 세팅하지 않음 (null 상태)
    MemberUpdateRequest request = new MemberUpdateRequest();
    ReflectionTestUtils.setField(request, "phone", "010-9999-8888");

    given(memberRepository.findActiveWithLockById(memberId)).willReturn(Optional.of(member));

    // when
    memberService.updateMember(memberId, request);

    // then
    // 전화번호는 변경되고, 나머지 필드는 기존 값 그대로 유지되어야 함
    assertThat(member.getPhone()).isEqualTo("010-9999-8888");
    assertThat(member.getUserName()).isEqualTo("oldUserName");
    assertThat(member.getName()).isEqualTo("oldName");
    assertThat(member.getEmail()).isEqualTo("old@email.com");
  }

  @Test
  @DisplayName("이름과 전화번호가 그대로면 회원 수정 이벤트를 발행하지 않는다")
  void updateMember_doesNotPublishEvent_whenNameAndPhoneAreUnchanged() {
    Long memberId = 1L;
    Member member =
        Member.builder()
            .userName("oldUserName")
            .password("oldPassword")
            .name("sameName")
            .phone("010-1111-2222")
            .email("old@email.com")
            .build();

    MemberUpdateRequest request = new MemberUpdateRequest();
    ReflectionTestUtils.setField(request, "userName", "newUserName");
    ReflectionTestUtils.setField(request, "email", "new@email.com");

    given(memberRepository.findActiveWithLockById(memberId)).willReturn(Optional.of(member));
    given(memberRepository.existsByUserName("newUserName")).willReturn(false);
    given(memberRepository.existsByEmail("new@email.com")).willReturn(false);

    memberService.updateMember(memberId, request);

    verify(memberEventWriter, never()).appendUpdated(member);
  }
}
