package kr.lastdish.payment.domain.event;

import java.math.BigDecimal;

public record ChargeRequestedPayload(Long memberId, Long paymentId, BigDecimal amount) {}
