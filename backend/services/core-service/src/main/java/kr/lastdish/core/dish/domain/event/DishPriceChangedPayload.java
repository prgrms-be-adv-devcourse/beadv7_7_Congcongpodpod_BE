package kr.lastdish.core.dish.domain.event;

import java.math.BigDecimal;

public record DishPriceChangedPayload(BigDecimal unitPrice) {}
