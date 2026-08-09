package kr.lastdish.member.member.presentation;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import kr.lastdish.member.auth.infrastructure.JwtTokenProvider;
import kr.lastdish.member.member.application.MemberService;
import kr.lastdish.member.member.application.dto.MemberProfileResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
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

  @Test
  void 주문에_필요한_회원_정보를_조회한다() throws Exception {
    LocalDateTime now = LocalDateTime.now();

    MemberProfileResult result =
        new MemberProfileResult(
            1L, "member1", "김영진", "010-1234-5678", "member@example.com", "MEMBER", now, now);

    when(memberService.getMemberById(1L)).thenReturn(result);

    mockMvc
        .perform(get("/internal/v1/members/{memberId}/order-info", 1L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.name").value("김영진"))
        .andExpect(jsonPath("$.data.phone").value("010-1234-5678"));

    verify(memberService).getMemberById(1L);
  }
}
