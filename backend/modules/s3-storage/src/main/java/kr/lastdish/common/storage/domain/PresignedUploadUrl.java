package kr.lastdish.common.storage.domain;

import java.net.URL;
import java.time.Instant;
import java.util.Map;

/**
 * 업로드 대상 Object Key와 Presigned PUT URL, 서명에 포함된 필수 헤더 및 만료 시각을 전달합니다.
 *
 * <p>클라이언트는 {@code requiredHeaders}를 실제 S3 PUT 요청에 동일하게 포함해야 합니다.
 */
public record PresignedUploadUrl(
    String objectKey, URL url, Map<String, String> requiredHeaders, Instant expiresAt) {

  public PresignedUploadUrl {
    requiredHeaders = Map.copyOf(requiredHeaders);
  }
}
