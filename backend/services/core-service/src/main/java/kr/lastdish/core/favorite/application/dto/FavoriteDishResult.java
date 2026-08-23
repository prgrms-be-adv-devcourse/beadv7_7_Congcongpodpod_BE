package kr.lastdish.core.favorite.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FavoriteDishResult(
    Long dishId,
    String dishName,
    LocalDateTime registeredAt,
    String description,
    String thumbnailUrl,
    Long stockQuantity,
    BigDecimal dishPrice,
    BigDecimal discountPrice) {}
