package kr.lastdish.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import kr.lastdish.member.auth.presentation.dto.SignUpRequest;
import kr.lastdish.member.member.domain.Member;
import kr.lastdish.member.member.domain.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MemberServiceApplicationTests {

  @Autowired private MockMvc mockMvc;

  @Autowired private MemberRepository memberRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  @DisplayName("통합 테스트: 회원가입을 요청하면 DB에 암호화된 비밀번호와 함께 데이터가 잘 적재되고 응답을 반환한다.")
  void signUpIntegrationTest() throws Exception {
    SignUpRequest request =
        new SignUpRequest(
            "testuser123",
            "securePassword123!",
            "테스트유저",
            "010-1234-5678",
            "testuser@gmail.com",
            "MEMBER");

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
}
