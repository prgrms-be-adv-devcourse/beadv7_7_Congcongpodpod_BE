package kr.lastdish.core.order.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record OrderCreateRequest(@NotBlank String phone) {}
