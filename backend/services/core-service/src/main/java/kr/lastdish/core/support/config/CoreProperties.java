package kr.lastdish.core.support.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "core")
public record CoreProperties(String message) {

    public CoreProperties {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("core.message must not be blank");
        }
    }
}
