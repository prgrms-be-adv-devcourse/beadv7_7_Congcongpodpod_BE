package kr.lastdish.core.dish.presentation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import kr.lastdish.core.dish.domain.Category;
import kr.lastdish.core.dish.domain.Dish;

public record DishCreateResponse(
    Long dishId,
    Long storeId,
    String dishName,
    LocalDateTime registeredAt,
    String description,
    Category category,
    String thumbnailUrl,
    Long stockQuantity,
    String dishStatus,
    BigDecimal dishPrice,
    BigDecimal discountPrice) {
  public static DishCreateResponse from(Dish dish) {
    return new DishCreateResponse(
        dish.getId(),
        dish.getStoreId(),
        dish.getDishName(),
        dish.getRegisteredAt(),
        dish.getDescription(),
        dish.getCategory(),
        dish.getThumbnailUrl(),
        dish.getStockQuantity(),
        dish.getDishStatus().name(),
        dish.getDishPrice(),
        dish.getDiscountPrice());
  }
}
