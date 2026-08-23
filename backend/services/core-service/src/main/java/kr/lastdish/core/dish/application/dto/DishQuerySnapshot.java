package kr.lastdish.core.dish.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DishQuerySnapshot(
    Long dishId,
    Long storeId,
    String dishName,
    LocalDateTime registeredAt,
    String description,
    String thumbnailUrl,
    Long stockQuantity,
    BigDecimal dishPrice,
    BigDecimal discountPrice) {}
