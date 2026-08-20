package kr.lastdish.core.dish.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Dish 이미지 업로드 URL 발급에 필요한 매장, MIME 타입과 실제 파일 크기를 전달합니다. */
public record DishImageUploadUrlRequest(
    @NotNull Long storeId, @NotBlank String contentType, @Positive long fileSize) {}
