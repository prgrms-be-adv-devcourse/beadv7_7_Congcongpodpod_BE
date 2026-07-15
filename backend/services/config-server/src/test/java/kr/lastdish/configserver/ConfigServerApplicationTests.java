package kr.lastdish.configserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.cloud.config.server.git.uri=file://${java.io.tmpdir}/config-repo",
        "spring.cloud.config.server.git.clone-on-start=false"
})
class ConfigServerApplicationTests {

    @Test
    void contextLoads() {
    }
}
