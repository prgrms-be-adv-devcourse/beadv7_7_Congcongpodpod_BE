package kr.lastdish.ai.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record PresignedUploadRequest(
        @NotBlank(message = "Content-Type은 필수입니다.")
        String contentType,

        @Positive(message = "파일 크기는 0보다 커야 합니다.")
        long contentLength
) {}