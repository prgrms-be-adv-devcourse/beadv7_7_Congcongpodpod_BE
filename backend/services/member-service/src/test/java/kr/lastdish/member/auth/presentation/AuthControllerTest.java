package kr.lastdish.member.auth.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.lastdish.member.auth.application.AuthService;
import kr.lastdish.member.auth.application.dto.SignUpResult;
import kr.lastdish.member.auth.application.dto.TokenResult;
import kr.lastdish.member.auth.domain.TokenProvider;
import kr.lastdish.member.auth.presentation.dto.LoginRequest;
import kr.lastdish.member.auth.presentation.dto.SignUpRequest;
import kr.lastdish.member.auth.presentation.dto.TokenRefreshRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

  @Autowired private MockMvc mockMvc;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @MockitoBean private AuthService authService;
  @MockitoBean private TokenProvider tokenProvider;
  @MockitoBean private PasswordEncoder passwordEncoder;
  @MockitoBean private CacheManager cacheManager;

  @Test
  @DisplayName("회원가입 요청 성공")
  void signUpTest() throws Exception {
    SignUpRequest signUpRequest =
        new SignUpRequest("testuser", "password123!", "테스터", "010-1234-5678", "test@example.com");

    SignUpResult mockSignUpResult = new SignUpResult(1L, "testuser", "test@example.com");
    given(authService.signUp(any())).willReturn(mockSignUpResult);

    mockMvc
        .perform(
            post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signUpRequest)))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("로그인 요청 성공 시 Access Token 및 Refresh Token 반환")
  void loginTest() throws Exception {
    LoginRequest loginRequest = new LoginRequest("test@example.com", "password123!");
    TokenResult mockTokenResult = new TokenResult("mock-access-token", "mock-refresh-token");

    given(authService.login(any())).willReturn(mockTokenResult);

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.accessToken").value("mock-access-token"))
        .andExpect(jsonPath("$.data.refreshToken").value("mock-refresh-token"));
  }

  @Test
  @DisplayName("토큰 재발급 요청 성공")
  void refreshTest() throws Exception {
    TokenRefreshRequest refreshRequest = new TokenRefreshRequest("mock-refresh-token");
    TokenResult mockTokenResult = new TokenResult("new-access-token", "new-refresh-token");

    given(authService.refresh(any())).willReturn(mockTokenResult);

    mockMvc
        .perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.accessToken").value("new-access-token"));
  }
}
