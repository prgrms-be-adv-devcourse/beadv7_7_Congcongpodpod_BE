package kr.lastdish.member.support.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "member")
public record MemberProperties(String message) {

    public MemberProperties {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("member.message must not be blank");
        }
    }
}
