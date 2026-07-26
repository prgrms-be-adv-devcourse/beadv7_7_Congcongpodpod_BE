package kr.lastdish.member.auth.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.lastdish.member.auth.presentation.dto.LoginRequest;
import kr.lastdish.member.auth.presentation.dto.SignUpRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Test
  @DisplayName("회원가입 후 로그인 및 토큰 재발급 통합 테스트")
  void signUpAndLoginAndRefreshTest() throws Exception {
    // given 1: 회원가입 요청 데이터
    SignUpRequest signUpRequest =
        new SignUpRequest(
            "testuser", "password123!", "테스터", "010-1234-5678", "test@example.com", "MEMBER");

    // when 1: 회원가입 API 호출
    mockMvc
        .perform(
            post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signUpRequest)))
        .andExpect(status().isOk());

    // given 2: 로그인 요청 데이터
    LoginRequest loginRequest = new LoginRequest("test@example.com", "password123!");

    // when 2: 로그인 API 호출 및 토큰 발급 확인
    MvcResult loginResult =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").exists())
            .andExpect(jsonPath("$.data.refreshToken").exists())
            .andReturn();

    // 응답받은 JSON에서 JsonPath를 이용해 리프레시 토큰 값 직접 추출
    String responseBody = loginResult.getResponse().getContentAsString();
    String refreshToken = com.jayway.jsonpath.JsonPath.read(responseBody, "$.data.refreshToken");

    // given 3: 리프레시 토큰으로 재발급 요청 데이터 준비
    String refreshJson = "{\"refreshToken\":\"" + refreshToken + "\"}";

    // when 3: 토큰 재발급 API 호출 및 새로운 Access Token 발급 확인
    mockMvc
        .perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshJson))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.accessToken").exists());
  }
}
