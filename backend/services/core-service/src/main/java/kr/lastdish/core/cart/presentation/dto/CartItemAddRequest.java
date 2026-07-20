package kr.lastdish.core.cart.presentation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CartItemAddRequest(@NotNull Long dishId, @Positive Long quantity) {}
