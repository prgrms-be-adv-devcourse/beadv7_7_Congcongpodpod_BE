package kr.lastdish.core.dish.presentation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import kr.lastdish.core.dish.domain.Dish;

public record DishResponse(
    Long dishId,
    Long storeId,
    String dishName,
    LocalDateTime registeredAt,
    String description,
    String thumbnailUrl,
    Long stockQuantity,
    String dishStatus,
    BigDecimal dishPrice,
    BigDecimal discountPrice) {
  public static DishResponse from(Dish dish) {
    return new DishResponse(
        dish.getId(),
        dish.getStoreId(),
        dish.getDishName(),
        dish.getRegisteredAt(),
        dish.getDescription(),
        dish.getThumbnailUrl(),
        dish.getStockQuantity(),
        dish.getDishStatus().name(),
        dish.getDishPrice(),
        dish.getDiscountPrice());
  }
}
