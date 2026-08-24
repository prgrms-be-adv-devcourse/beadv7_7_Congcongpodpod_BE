package kr.lastdish.core.settlement.application.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderNoShowPayload(
    Long storeId, BigDecimal salesAmount, LocalDateTime pickupResultAt) {}
