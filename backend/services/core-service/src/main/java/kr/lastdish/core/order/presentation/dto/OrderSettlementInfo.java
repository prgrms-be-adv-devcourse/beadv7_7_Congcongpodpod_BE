package kr.lastdish.core.order.presentation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderSettlementInfo(
    Long orderId, Long storeId, BigDecimal salesAmount, LocalDateTime orderCompletedAt) {}
