package kr.lastdish.ai.infrastructure.event.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record DishEventPayload(
    Long dishId,
    Long storeId,
    String storeName,
    String dishName,
    LocalDateTime registeredAt,
    String description,
    String thumbnailUrl,
    Long stockQuantity,
    String dishStatus,
    BigDecimal dishPrice,
    BigDecimal discountPrice,
    LocalTime pickupStartTime,
    LocalTime pickupEndTime) {}
