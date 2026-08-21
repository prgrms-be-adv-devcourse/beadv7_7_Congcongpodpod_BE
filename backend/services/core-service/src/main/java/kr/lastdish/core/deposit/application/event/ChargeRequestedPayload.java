package kr.lastdish.core.deposit.application.event;

import java.math.BigDecimal;

public record ChargeRequestedPayload(Long memberId, BigDecimal amount) {}
