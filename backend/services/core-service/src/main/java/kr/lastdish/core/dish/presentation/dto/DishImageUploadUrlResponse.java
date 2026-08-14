package kr.lastdish.core.dish.presentation.dto;

import java.time.Instant;
import java.util.Map;
import kr.lastdish.core.storage.application.dto.PresignedUploadUrl;

public record DishImageUploadUrlResponse(
    String key, String uploadUrl, Map<String, String> requiredHeaders, Instant expiresAt) {

  public static DishImageUploadUrlResponse from(PresignedUploadUrl result) {
    return new DishImageUploadUrlResponse(
        result.objectKey(),
        result.url().toExternalForm(),
        result.requiredHeaders(),
        result.expiresAt());
  }
}
