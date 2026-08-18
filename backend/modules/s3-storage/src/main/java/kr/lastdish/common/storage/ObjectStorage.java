package kr.lastdish.common.storage;

import java.time.Duration;

public interface ObjectStorage {

  PresignedUploadUrl issuePutUrl(
      String objectKey, String contentType, long contentLength, Duration expiration);

  PresignedDownloadUrl issueGetUrl(String objectKey, Duration expiration);

  StoredObjectMetadata getMetadata(String objectKey);

  void copy(String sourceKey, String destinationKey);

  void delete(String objectKey);
}
