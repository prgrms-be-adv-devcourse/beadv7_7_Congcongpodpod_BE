package kr.lastdish.core.dish.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import kr.lastdish.core.dish.domain.Dish;

public record InternalDishResult(
    Long dishId,
    Long storeId,
    String dishName,
    String category,
    LocalDateTime registeredAt,
    String description,
    String thumbnailUrl,
    Long stockQuantity,
    String dishStatus,
    BigDecimal dishPrice,
    BigDecimal discountPrice,
    LocalTime pickupStartTime,
    LocalTime pickupEndTime) {
  public static InternalDishResult from(Dish dish) {
    return new InternalDishResult(
        dish.getId(),
        dish.getStoreId(),
        dish.getDishName(),
        dish.getCategory(),
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
