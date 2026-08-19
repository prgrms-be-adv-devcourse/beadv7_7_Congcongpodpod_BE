package kr.lastdish.core.store.presentation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
  public static InternalDishResponse from(Dish dish) {
    return new InternalDishResponse(
        dish.getId(),
        dish.getStoreId(),
        dish.getDishName(),
        dish.getRegisteredAt(),
        dish.getDescription(),
        dish.getThumbnailUrl(),
        dish.getStockQuantity(),
        dish.getDishStatus().name(),
        dish.getDishPrice(),
        dish.getDiscountPrice(),
        dish.getPickupStartTime(),
        dish.getPickupEndTime());
  }
}
