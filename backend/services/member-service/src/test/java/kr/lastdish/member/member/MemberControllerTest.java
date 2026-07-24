package kr.lastdish.member.member;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.lastdish.member.auth.presentation.dto.LoginRequest;
import kr.lastdish.member.auth.presentation.dto.SignUpRequest;
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
            "lookupuser", "password123!", "조회테스터", "010-1234-9999", "lookup@example.com", "MEMBER");

    mockMvc
        .perform(
            post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signUpRequest)))
        .andExpect(status().isOk());

    // given: 2. 로그인 요청 시 올바른 이메일 형식 전달
    LoginRequest loginRequest = new LoginRequest("lookup@example.com", "password123!");
    String loginResponseBody =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    // 3. 로그인 응답(ApiResponse)의 data 내부에서 accessToken 필드 추출
    String accessToken =
        objectMapper.readTree(loginResponseBody).path("data").path("accessToken").asText();

    // when & then: 4. 토큰을 담아 내 프로필 조회 API 호출
    mockMvc
        .perform(get("/api/members/me").header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.userName").value("lookupuser"))
        .andExpect(jsonPath("$.data.name").value("조회테스터"))
        .andExpect(jsonPath("$.data.email").value("lookup@example.com"));
  }
}
