package kr.lastdish.core.support.presentation;

import java.util.Map;

import kr.lastdish.core.support.config.CoreProperties;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/core")
public class CoreProbeController {

    private final CoreProperties coreProperties;

    public CoreProbeController(CoreProperties coreProperties) {
        this.coreProperties = coreProperties;
    }

    @GetMapping("/hello")
    public Map<String, String> hello() {
        return Map.of(
                "service", "core-service",
                "message", coreProperties.message()
        );
    }
}
