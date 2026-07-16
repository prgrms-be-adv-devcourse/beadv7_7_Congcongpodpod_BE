package kr.lastdish.member.support.presentation;

import java.util.Map;
import kr.lastdish.member.support.config.MemberProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
public class MemberProbeController {

  private final MemberProperties memberProperties;

  public MemberProbeController(MemberProperties memberProperties) {
    this.memberProperties = memberProperties;
  }

  @GetMapping("/hello")
  public Map<String, String> hello() {
    return Map.of("service", "member-service", "message", memberProperties.message());
  }
}
