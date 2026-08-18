package kr.lastdish.core.dish.presentation.dto;

import java.time.Instant;
import kr.lastdish.common.storage.PresignedDownloadUrl;

/** 클라이언트가 Dish 이미지를 조회할 때 사용할 Object Key, URL과 만료 시각을 반환합니다. */
public record DishImageDownloadUrlResponse(String key, String downloadUrl, Instant expiresAt) {

  public static DishImageDownloadUrlResponse from(PresignedDownloadUrl result) {
    return new DishImageDownloadUrlResponse(
        result.objectKey(), result.url().toExternalForm(), result.expiresAt());
  }
}
