package kr.lastdish.core.dish.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import kr.lastdish.core.dish.domain.Category;

public record DIshUpdateRequest(
    @NotNull Long dishId,
    @NotBlank String dishName,
    @NotNull LocalDateTime registeredAt,
    String description,
    @NotNull Category category,
    String thumbnailUrl,
    @NotNull @PositiveOrZero Long stockQuantity,
    @NotNull @Positive BigDecimal dishPrice,
    @NotNull @PositiveOrZero BigDecimal discountPrice) {}
