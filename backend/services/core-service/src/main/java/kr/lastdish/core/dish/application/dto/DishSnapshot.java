package kr.lastdish.core.dish.application.dto;

import java.math.BigDecimal;

public record DishSnapshot(
    Long dishId, String dishName, BigDecimal unitPrice, Long stockQuantity) {}
