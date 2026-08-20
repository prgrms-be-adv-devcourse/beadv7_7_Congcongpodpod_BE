package kr.lastdish.payment.exception;

import kr.lastdish.common.api.exception.ErrorCodeSpec;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode implements ErrorCodeSpec {
    INVALID_PAYMENT_STATUS(HttpStatus.CONFLICT, "PAY001", "결제 대기 상태에서만 처리할 수 있습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}