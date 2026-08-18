package kr.lastdish.core.dish.presentation.dto;

import java.time.Instant;
import kr.lastdish.common.storage.PresignedDownloadUrl;

public record DishImageDownloadUrlResponse(String key, String downloadUrl, Instant expiresAt) {

  public static DishImageDownloadUrlResponse from(PresignedDownloadUrl result) {
    return new DishImageDownloadUrlResponse(
        result.objectKey(), result.url().toExternalForm(), result.expiresAt());
  }
}
