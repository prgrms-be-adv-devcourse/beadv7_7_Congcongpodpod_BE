package kr.lastdish.core.dish.domain.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import kr.lastdish.core.dish.domain.DishStatus;

public record DishCreatedPayload(
    Long dishId,
    Long storeId,
    String dishName,
    LocalDateTime registeredAt,
    String description,
    String thumbnailUrl,
    Long stockQuantity,
    DishStatus dishStatus,
    BigDecimal dishPrice,
    BigDecimal discountPrice,
    LocalTime pickupStartTime,
    LocalTime pickupEndTime) {}
