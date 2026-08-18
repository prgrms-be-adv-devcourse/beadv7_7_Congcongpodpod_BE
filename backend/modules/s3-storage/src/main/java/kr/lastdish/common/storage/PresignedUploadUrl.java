package kr.lastdish.common.storage;

import java.net.URL;
import java.time.Instant;
import java.util.Map;

public record PresignedUploadUrl(
    String objectKey, URL url, Map<String, String> requiredHeaders, Instant expiresAt) {

  public PresignedUploadUrl {
    requiredHeaders = Map.copyOf(requiredHeaders);
  }
}
