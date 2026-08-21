package kr.lastdish.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import kr.lastdish.member.auth.presentation.dto.LoginRequest;
import kr.lastdish.member.auth.presentation.dto.SignUpRequest;
import kr.lastdish.member.member.domain.Member;
import kr.lastdish.member.member.domain.MemberRepository;
import kr.lastdish.member.member.presentation.dto.MemberUpdateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MemberServiceApplicationTests {

  @MockitoBean(name = "redisTemplate")
  private RedisTemplate<String, String> redisTemplate;

  @MockitoBean(name = "notificationRedisTemplate")
  private RedisTemplate<String, String> notificationRedisTemplate;

  @MockitoBean(name = "notificationRedisContainer")
  private RedisMessageListenerContainer notificationRedisContainer;

  @Autowired private MockMvc mockMvc;

  @Autowired private MemberRepository memberRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  @DisplayName("통합 테스트: 회원가입을 요청하면 DB에 암호화된 비밀번호와 함께 데이터가 잘 적재되고 응답을 반환한다.")
  void signUpIntegrationTest() throws Exception {
    SignUpRequest request =
        new SignUpRequest(
            "testuser123", "securePassword123!", "테스트유저", "010-1234-5678", "testuser@gmail.com");

    String requestJson = objectMapper.writeValueAsString(request);

    mockMvc
        .perform(
            post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
        .andExpect(status().isOk());

    Optional<Member> savedMemberOpt = memberRepository.findByUserName("testuser123");

    assertThat(savedMemberOpt).isPresent();
    Member savedMember = savedMemberOpt.get();

    assertThat(savedMember.getPassword()).isNotEqualTo("securePassword123!");
    assertThat(passwordEncoder.matches("securePassword123!", savedMember.getPassword())).isTrue();
  }

  @Test
  @DisplayName("통합 테스트: 회원가입 후 올바른 정보로 로그인을 요청하면 Access Token과 Refresh Token이 발급된다.")
  void loginIntegrationTest() throws Exception {
    // given (로그인을 위한 회원가입 사전 작업)
    SignUpRequest signUpRequest =
        new SignUpRequest(
            "loginuser123", "securePassword123!", "로그인유저", "010-9876-5432", "loginuser@gmail.com");

    String signUpJson = objectMapper.writeValueAsString(signUpRequest);

    // 1. 회원가입 수행
    mockMvc
        .perform(
            post("/api/v1/auth/signup").contentType(MediaType.APPLICATION_JSON).content(signUpJson))
        .andExpect(status().isOk());

    // 2. 로그인 요청 객체 생성
    LoginRequest loginRequest = new LoginRequest("loginuser@gmail.com", "securePassword123!");
    String loginJson = objectMapper.writeValueAsString(loginRequest);

    // when & then (로그인 API 호출 및 토큰 응답 검증)
    mockMvc
        .perform(
            post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginJson))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.accessToken").exists())
        .andExpect(jsonPath("$.data.refreshToken").exists());
  }

  @Test
  @DisplayName("통합 테스트: 회원가입 후 비밀번호 변경 및 부분 수정 확인")
  void updateMemberIntegrationTest() throws Exception {
    // 1. 회원가입
    SignUpRequest signUpRequest =
        new SignUpRequest("user123", "oldPassword123!", "테스터", "010-1111-2222", "user@gmail.com");
    mockMvc
        .perform(
            post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signUpRequest)))
        .andExpect(status().isOk());

    Long memberId = memberRepository.findByUserName("user123").get().getId();

    // 2. 부분 업데이트: 전화번호만 변경
    MemberUpdateRequest partialUpdateRequest = new MemberUpdateRequest();
    ReflectionTestUtils.setField(partialUpdateRequest, "phone", "010-9999-8888");

    mockMvc
        .perform(
            put("/api/v1/members/me")
                .header("X-Authenticated-Member-Id", memberId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(partialUpdateRequest)))
        .andExpect(status().isOk());

    // 3. 검증: 전화번호는 바뀌고 나머지 정보는 유지되었는지 확인
    Member updatedMember = memberRepository.findById(memberId).get();
    assertThat(updatedMember.getPhone()).isEqualTo("010-9999-8888");
    assertThat(updatedMember.getName()).isEqualTo("테스터");
  }

  @Test
  @DisplayName("통합 테스트: 비밀번호를 수정하면 새 비밀번호로 로그인이 성공하고 구 비밀번호로는 실패한다.")
  void updatePasswordAndLoginIntegrationTest() throws Exception {
    // 1. 회원가입
    SignUpRequest signUpRequest =
        new SignUpRequest(
            "pwuser123", "oldPassword123!", "비번유저", "010-1111-2222", "pwuser@gmail.com");
    mockMvc
        .perform(
            post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signUpRequest)))
        .andExpect(status().isOk());

    Long memberId = memberRepository.findByUserName("pwuser123").get().getId();

    // 2. 비밀번호 변경 (PUT /api/v1/members/me)
    MemberUpdateRequest passwordUpdateRequest = new MemberUpdateRequest();
    ReflectionTestUtils.setField(passwordUpdateRequest, "password", "newPassword456!");

    mockMvc
        .perform(
            put("/api/v1/members/me")
                .header("X-Authenticated-Member-Id", memberId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(passwordUpdateRequest)))
        .andExpect(status().isOk());

    // 3. 새 비밀번호로 로그인 성공 테스트
    LoginRequest newLoginRequest = new LoginRequest("pwuser@gmail.com", "newPassword456!");
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newLoginRequest)))
        .andExpect(status().isOk());

    // 4. 구 비밀번호로 로그인 실패 테스트 (401)
    LoginRequest oldLoginRequest = new LoginRequest("pwuser@gmail.com", "oldPassword123!");
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(oldLoginRequest)))
        .andExpect(status().isUnauthorized());
  }
}
