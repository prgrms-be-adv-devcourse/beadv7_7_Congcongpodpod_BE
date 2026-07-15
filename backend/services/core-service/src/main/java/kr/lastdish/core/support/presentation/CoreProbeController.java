package kr.lastdish.core.support.presentation;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/core")
public class CoreProbeController {

    @GetMapping("/hello")
    public Map<String, String> hello() {
        return Map.of(
                "service", "core-service",
                "message", "Hello from core-service"
        );
    }
}
