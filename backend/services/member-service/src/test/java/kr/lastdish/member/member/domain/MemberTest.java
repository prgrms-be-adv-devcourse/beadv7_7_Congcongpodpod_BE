package kr.lastdish.member.member.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MemberTest {

  private Member member;

  @BeforeEach
  void setUp() {
    member =
        Member.builder()
            .userName("member01")
            .password("encoded-password")
            .name("회원")
            .phone("01012345678")
            .email("member@example.com")
            .role(Role.MEMBER)
            .build();
  }

  @Test
  void MEMBER를_SELLER로_변경한다() {
    member.promoteToSeller();

    assertThat(member.getRole()).isEqualTo(Role.SELLER);
  }

  @Test
  void 이미_SELLER인_회원도_멱등하게_처리한다() {
    member.promoteToSeller();
    member.promoteToSeller();

    assertThat(member.getRole()).isEqualTo(Role.SELLER);
  }
}
