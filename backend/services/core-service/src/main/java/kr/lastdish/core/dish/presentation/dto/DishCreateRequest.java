package kr.lastdish.core.dish.presentation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record DishCreateRequest(
    @NotNull Long storeId,
    @NotBlank String dishName,
    @NotNull LocalDateTime registeredAt,
    @NotBlank String description,
    String thumbnailUrl,
    @NotNull @Positive Long stockQuantity,
    @NotNull @Positive BigDecimal dishPrice,
    @NotNull @PositiveOrZero BigDecimal discountPrice,
    @JsonFormat(pattern = "HH:mm") LocalTime pickupStartTime,
    @JsonFormat(pattern = "HH:mm") LocalTime pickupEndTime) {}
