package kr.lastdish.core.store.presentation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import kr.lastdish.core.dish.presentation.dto.DishResponse;

public record StoreDishResponse(
    Long dishId,
    String dishName,
    LocalDateTime registeredAt,
    String description,
    String thumbnailUrl,
    Long stockQuantity,
    BigDecimal dishPrice,
    BigDecimal discountPrice) {

  public static StoreDishResponse from(DishResponse dish) {
    return new StoreDishResponse(
        dish.dishId(),
        dish.dishName(),
        dish.registeredAt(),
        dish.description(),
        dish.thumbnailUrl(),
        dish.stockQuantity(),
        dish.dishPrice(),
        dish.discountPrice());
  }
}
