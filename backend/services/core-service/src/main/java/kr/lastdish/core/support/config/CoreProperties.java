package kr.lastdish.core.support.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;

@ConfigurationProperties(prefix = "core")
@RefreshScope
public class CoreProperties {

    private String message;

    public String message() {
        return message;
    }

    public void setMessage(String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("core.message must not be blank");
        }
        this.message = message;
    }
}
