package kr.lastdish.core.dish.application.dto;

import java.math.BigDecimal;
import java.time.LocalTime;

public record DishSnapshot(
    Long dishId,
    Long storeId,
    String dishName,
    BigDecimal unitPrice,
    BigDecimal totalPrice,
    BigDecimal savedAmount,
    Long stockQuantity,
    LocalTime pickupStartAt,
    LocalTime pickupEndAt,
    long aggregateVersion) {}
