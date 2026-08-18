package kr.lastdish.common.storage;

import java.time.Duration;

public interface ObjectStorage {

  PresignedUploadUrl issuePutUrl(
      String objectKey, String contentType, long contentLength, Duration expiration);

  StoredObjectMetadata getMetadata(String objectKey);

  void copy(String sourceKey, String destinationKey);
}
