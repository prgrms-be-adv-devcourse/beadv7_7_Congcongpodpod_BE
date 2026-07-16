package kr.lastdish.member.support.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;

@ConfigurationProperties(prefix = "member")
@RefreshScope
public class MemberProperties {

  private String message;

  public String message() {
    return message;
  }

  public void setMessage(String message) {
    if (message == null || message.isBlank()) {
      throw new IllegalArgumentException("member.message must not be blank");
    }
    this.message = message;
  }
}
