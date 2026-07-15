package kr.lastdish.core.support.presentation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CoreProbeControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsCoreServiceIdentity() throws Exception {
        mockMvc.perform(get("/api/core/hello"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("core-service"))
                .andExpect(jsonPath("$.message").value("Hello from test config"));
    }
}
