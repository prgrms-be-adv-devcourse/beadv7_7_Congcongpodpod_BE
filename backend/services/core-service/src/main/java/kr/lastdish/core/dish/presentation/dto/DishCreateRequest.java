package kr.lastdish.core.dish.presentation.dto;

import kr.lastdish.core.dish.domain.Category;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DishCreateRequest(
        Long storeId,
        String dishName,
        LocalDateTime registeredAt,
        String description,
        Category category,
        String thumbnailUrl,
        Long stockQuantity,
        BigDecimal dishPrice,
        BigDecimal discountPrice
) {

}
