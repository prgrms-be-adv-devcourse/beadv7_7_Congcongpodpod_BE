package kr.lastdish.core.cart.presentation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CartItemUpdateRequest(@NotNull @Min(1) Long quantity) {}
