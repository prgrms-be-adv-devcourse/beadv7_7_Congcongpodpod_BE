package kr.lastdish.ai.infrastructure.client.dto;

import java.math.BigDecimal;
import java.time.LocalTime;

public record InternalDishResponse(
    Long dishId,
    String dishName,
    String description,
    String category,
    String thumbnailUrl,
    Long stockQuantity,
    String dishStatus,
    BigDecimal dishPrice,
    BigDecimal discountPrice,
    LocalTime pickupStartTime,
    LocalTime pickupEndTime) {}
