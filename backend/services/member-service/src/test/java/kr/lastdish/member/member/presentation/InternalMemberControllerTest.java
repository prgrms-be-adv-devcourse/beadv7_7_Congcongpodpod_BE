package kr.lastdish.member.member.presentation;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import kr.lastdish.member.auth.infrastructure.JwtTokenProvider;
import kr.lastdish.member.member.application.MemberService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InternalMemberController.class)
class InternalMemberControllerTest {

  @Autowired MockMvc mockMvc;

  @MockitoBean MemberService memberService;

  @MockitoBean JwtTokenProvider jwtTokenProvider;

  @Test
  void 판매자_승급을_요청한다() throws Exception {
    mockMvc
        .perform(patch("/internal/v1/members/{memberId}/seller-role", 1L))
        .andExpect(status().isOk());

    verify(memberService).grantSellerRole(1L);
  }
}
