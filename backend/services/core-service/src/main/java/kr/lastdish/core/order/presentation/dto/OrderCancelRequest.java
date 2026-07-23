package kr.lastdish.core.order.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OrderCancelRequest(@NotNull @NotBlank String cancelReason) {}
