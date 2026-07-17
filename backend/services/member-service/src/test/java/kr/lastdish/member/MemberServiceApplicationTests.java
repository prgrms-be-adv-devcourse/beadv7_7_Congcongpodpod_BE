package kr.lastdish.member;

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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MemberServiceApplicationTests {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private MemberRepository memberRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;


  private final ObjectMapper objectMapper = new ObjectMapper();
  @Test
  @DisplayName("통합 테스트: 회원가입을 요청하면 DB에 암호화된 비밀번호와 함께 데이터가 잘 적재되고 응답을 반환한다.")
  void signUpIntegrationTest() throws Exception {
    // given: 실제 SignUpRequest 레코드 스펙 순서에 완벽히 맞춘 요청 데이터 정의
    SignUpRequest request = new SignUpRequest(
            "testuser123",          // 1. userName (아이디: 4자 이상 20자 이하)
            "securePassword123!",   // 2. password (비밀번호: 최소 8자 이상)
            "테스트유저",             // 3. name (이름)
            "010-1234-5678",        // 4. phone (전화번호)
            "testuser@gmail.com",   // 5. email (이메일 형식이 올바름)
            "MEMBER"                // 6. role (역할 필수값)
    );

    String requestJson = objectMapper.writeValueAsString(request);

    mockMvc.perform(post("/members/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson))
            .andExpect(status().isOk());

    Optional<Member> savedMemberOpt = memberRepository.findByUserName("testuser123");

    assertThat(savedMemberOpt).isPresent();
    Member savedMember = savedMemberOpt.get();

    // 암호화 검증
    assertThat(savedMember.getPassword()).isNotEqualTo("securePassword123!");
    assertThat(passwordEncoder.matches("securePassword123!", savedMember.getPassword())).isTrue();
  }
}
