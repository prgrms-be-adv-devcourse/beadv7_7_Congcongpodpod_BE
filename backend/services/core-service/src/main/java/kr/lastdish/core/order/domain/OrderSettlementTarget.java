package kr.lastdish.core.order.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderSettlementTarget(
        Long id,
        Long storeId,
        BigDecimal totalPrice,
        LocalDateTime updatedAt
) {}
