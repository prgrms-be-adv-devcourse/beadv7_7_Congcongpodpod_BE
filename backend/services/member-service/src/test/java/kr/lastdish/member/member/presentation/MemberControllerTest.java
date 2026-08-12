package kr.lastdish.member.member.presentation;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import kr.lastdish.member.auth.domain.TokenProvider;
import kr.lastdish.member.member.application.MemberService;
import kr.lastdish.member.member.application.dto.MemberProfileResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = MemberController.class)
@AutoConfigureMockMvc(addFilters = false)
class MemberControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private MemberService memberService;
  @MockitoBean private TokenProvider tokenProvider;
  @MockitoBean private PasswordEncoder passwordEncoder;
  @MockitoBean private CacheManager cacheManager;

  @Test
  @DisplayName("내 프로필을 성공적으로 조회한다.")
  void getMyProfileSuccess() throws Exception {
    // given
    Long memberId = 1L;
    LocalDateTime now = LocalDateTime.now();

    MemberProfileResult mockResult =
        new MemberProfileResult(
            memberId,
            "lookupuser",
            "조회테스터",
            "010-1234-9999",
            "lookup@example.com",
            "MEMBER",
            now,
            now);

    given(memberService.getMemberById(memberId)).willReturn(mockResult);

    // when & then
    mockMvc
        .perform(get("/api/v1/members/me").header("X-Authenticated-Member-Id", memberId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.userName").value("lookupuser"))
        .andExpect(jsonPath("$.data.name").value("조회테스터"))
        .andExpect(jsonPath("$.data.email").value("lookup@example.com"));
  }
}
