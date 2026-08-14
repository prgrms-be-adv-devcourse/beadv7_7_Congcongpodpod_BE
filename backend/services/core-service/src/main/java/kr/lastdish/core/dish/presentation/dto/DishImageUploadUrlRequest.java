package kr.lastdish.core.dish.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record DishImageUploadUrlRequest(
    @NotNull Long storeId,
    @NotBlank String contentType,
    @Positive long fileSize) {}
