package kr.lastdish.core.order.domain.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderNoShowPayload(
    Long orderId, Long storeId, BigDecimal salesAmount, LocalDateTime pickupResultAt) {}
