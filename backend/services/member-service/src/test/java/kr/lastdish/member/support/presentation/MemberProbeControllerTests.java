package kr.lastdish.member.support.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class MemberProbeControllerTests {

  @Autowired private MockMvc mockMvc;

  @Test
  void returnsMemberServiceIdentity() throws Exception {
    mockMvc
        .perform(get("/internal/probe"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.service").value("member-service"))
        .andExpect(jsonPath("$.message").value("Hello from test config"));
  }

  @Test
  void exposesRefreshEndpoint() throws Exception {
    mockMvc.perform(post("/actuator/refresh")).andExpect(status().isOk());
  }
}
