package kr.lastdish.core.order.domain.event;

import java.math.BigDecimal;

public record OrderNoShowPayload(Long storeId, BigDecimal salesAmount) {}
