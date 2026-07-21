package kr.lastdish.core.order.presentation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalTime;

public record OrderCreateRequest(
        @NotNull Long storeId,
        @NotNull Long dishId,
        // String memberName, // 추후 이름 추가
        @NotBlank String phone,
        @NotBlank String dishName,
        @NotNull @Positive Long quantity,
        @NotNull @Positive BigDecimal unitPrice,
        @NotNull LocalTime pickupStartAt,
        @NotNull LocalTime pickupEndAt) {}
