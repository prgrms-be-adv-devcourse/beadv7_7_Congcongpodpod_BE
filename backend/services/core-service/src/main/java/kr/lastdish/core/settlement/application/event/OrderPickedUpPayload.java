package kr.lastdish.core.settlement.application.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderPickedUpPayload(
        Long orderId, Long memberId, Long storeId, BigDecimal finalOrderAmount, BigDecimal savedAmount, LocalDateTime pickupResultAt) {}
