package kr.lastdish.core.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ErrorCode {
    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
