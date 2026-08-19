package kr.lastdish.core.dish.presentation.dto;

import java.time.Instant;
import java.util.Map;
import kr.lastdish.common.storage.domain.PresignedUploadUrl;

/** 클라이언트가 S3에 직접 업로드할 때 사용할 Object Key, URL, 필수 헤더와 만료 시각을 반환합니다. */
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
