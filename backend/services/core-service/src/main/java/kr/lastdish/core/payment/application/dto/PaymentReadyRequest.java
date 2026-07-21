package kr.lastdish.core.payment.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import kr.lastdish.core.payment.domain.payment.PgProvider;

import java.math.BigDecimal;

public record PaymentReadyRequest(
        @NotNull(message = "결제 금액은 필수입니다.")
        @DecimalMin(value = "0", inclusive = false, message = "결제 금액은 0보다 커야 합니다.")
        BigDecimal amount,

        @NotNull(message = "결제 수단(PG사)은 필수입니다.")
        PgProvider pgProvider

) {}

