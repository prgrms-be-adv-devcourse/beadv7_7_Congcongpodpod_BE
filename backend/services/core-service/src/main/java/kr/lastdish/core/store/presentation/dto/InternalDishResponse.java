package kr.lastdish.core.store.presentation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

import kr.lastdish.core.dish.application.dto.InternalDishResult;
import kr.lastdish.core.dish.domain.Dish;

public record InternalDishResponse(
    Long dishId,
    Long storeId,
    String dishName,
    LocalDateTime registeredAt,
    String description,
    String thumbnailUrl,
    Long stockQuantity,
    String dishStatus,
    BigDecimal dishPrice,
    BigDecimal discountPrice,
    LocalTime pickupStartTime,
    LocalTime pickupEndTime) {
  public static InternalDishResponse from(InternalDishResult dish) {
    return new InternalDishResponse(
        dish.dishId(),
        dish.storeId(),
        dish.dishName(),
        dish.registeredAt(),
        dish.description(),
        dish.thumbnailUrl(),
        dish.stockQuantity(),
        dish.dishStatus(),
        dish.dishPrice(),
        dish.discountPrice(),
        dish.pickupStartTime(),
        dish.pickupEndTime());
  }
}
