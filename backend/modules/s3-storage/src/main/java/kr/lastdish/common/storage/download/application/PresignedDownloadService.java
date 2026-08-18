package kr.lastdish.common.storage.download.application;

import java.util.Optional;
import kr.lastdish.common.storage.ObjectStorage;
import kr.lastdish.common.storage.ObjectStorageException;
import kr.lastdish.common.storage.PresignedDownloadUrl;
import kr.lastdish.common.storage.download.domain.PresignedDownloadException;
import kr.lastdish.common.storage.s3.S3StorageProperties;

public class PresignedDownloadService {

  private final Optional<ObjectStorage> objectStorage;
  private final S3StorageProperties properties;

  public PresignedDownloadService(
      Optional<ObjectStorage> objectStorage, S3StorageProperties properties) {
    this.objectStorage = objectStorage;
    this.properties = properties;
  }

  public PresignedDownloadUrl issue(String objectKey) {
    try {
      return getObjectStorage().issueGetUrl(objectKey, properties.presignedUrlExpiration());
    } catch (ObjectStorageException exception) {
      throw new PresignedDownloadException(
          PresignedDownloadException.Reason.STORAGE_ERROR, exception);
    }
  }

  private ObjectStorage getObjectStorage() {
    return objectStorage.orElseThrow(
        () -> new PresignedDownloadException(PresignedDownloadException.Reason.STORAGE_DISABLED));
  }
}
