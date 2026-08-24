package kr.lastdish.core.favorite.presentation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import kr.lastdish.core.favorite.application.dto.FavoriteDishResult;

public record FavoriteDishResponse(
    Long dishId,
    String dishName,
    LocalDateTime registeredAt,
    String description,
    String thumbnailUrl,
    Long stockQuantity,
    BigDecimal dishPrice,
    BigDecimal discountPrice) {

  public static FavoriteDishResponse from(FavoriteDishResult result) {
    return new FavoriteDishResponse(
        result.dishId(),
        result.dishName(),
        result.registeredAt(),
        result.description(),
        result.thumbnailUrl(),
        result.stockQuantity(),
        result.dishPrice(),
        result.discountPrice());
  }
}
