package kr.lastdish.member.member;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.lastdish.member.auth.presentation.dto.SignUpRequest;
import kr.lastdish.member.member.domain.Member;
import kr.lastdish.member.member.domain.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class MemberControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private MemberRepository memberRepository;

  @Autowired private ObjectMapper objectMapper;

  @Test
  @DisplayName("내 프로필을 성공적으로 조회한다.")
  void getMyProfileSuccess() throws Exception {
    // given: 1. 회원가입 요청
    SignUpRequest signUpRequest =
        new SignUpRequest(
            "lookupuser", "password123!", "조회테스터", "010-1234-9999", "lookup@example.com");

    mockMvc
        .perform(
            post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signUpRequest)))
        .andExpect(status().isOk());

    Member member = memberRepository.findByEmail("lookup@example.com").orElseThrow();

    // when & then: Gateway가 생성한 인증 헤더로 내 프로필 조회 API 호출
    mockMvc
        .perform(get("/api/v1/members/me").header("X-Authenticated-Member-Id", member.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.userName").value("lookupuser"))
        .andExpect(jsonPath("$.data.name").value("조회테스터"))
        .andExpect(jsonPath("$.data.email").value("lookup@example.com"));
  }
}
