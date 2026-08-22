package kr.lastdish.core.dish.presentation.dto;

import jakarta.validation.constraints.NotNull;

public record DishStockAdjustRequest(@NotNull Long quantityDelta) {}
