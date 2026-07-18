package kr.lastdish.member.support.presentation;

import io.swagger.v3.oas.annotations.Hidden;
import java.util.Map;
import kr.lastdish.member.support.config.MemberProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/probe")
@Hidden
public class MemberProbeController {

  private final MemberProperties memberProperties;

  public MemberProbeController(MemberProperties memberProperties) {
    this.memberProperties = memberProperties;
  }

  @GetMapping
  public Map<String, String> hello() {
    return Map.of("service", "member-service", "message", memberProperties.message());
  }
}
