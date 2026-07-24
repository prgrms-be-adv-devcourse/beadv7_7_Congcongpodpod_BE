package kr.lastdish.core.payment.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record PaymentApproveRequest(
    @NotBlank(message = "paymentKey는 필수입니다.") String paymentKey,

    // READY 단계에서 생성한 merchantOrderId. 결제 건을 구분하는 내부 식별자
    @NotBlank(message = "orderId는 필수입니다.") String orderId,
    @NotNull(message = "결제 금액은 필수입니다.") @Positive(message = "결제 금액은 0보다 커야 합니다.")
        BigDecimal amount) {}
