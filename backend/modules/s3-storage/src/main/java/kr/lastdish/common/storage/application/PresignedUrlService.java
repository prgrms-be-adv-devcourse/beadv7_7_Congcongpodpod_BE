package kr.lastdish.common.storage.application;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import kr.lastdish.common.storage.domain.ObjectStorage;
import kr.lastdish.common.storage.domain.ObjectStorageException;
import kr.lastdish.common.storage.domain.PresignedDownloadUrl;
import kr.lastdish.common.storage.domain.PresignedUploadUrl;
import kr.lastdish.common.storage.domain.StoredObjectMetadata;
import kr.lastdish.common.storage.domain.PresignedUpload;
import kr.lastdish.common.storage.domain.PresignedUploadRepository;
import kr.lastdish.common.storage.domain.PresignedUrlException;
import kr.lastdish.common.storage.domain.UploadResourceType;
import kr.lastdish.common.storage.domain.UploadStatus;
import kr.lastdish.common.storage.image.ImageContentType;
import kr.lastdish.common.storage.image.UnsupportedImageContentTypeException;
import kr.lastdish.common.storage.infrastructure.s3.S3StorageProperties;
import org.springframework.transaction.annotation.Transactional;

/** Presigned 업로드 URL 발급·확정과 다운로드 URL 발급을 담당하는 공통 서비스입니다. */
public class PresignedUrlService {

  private final Optional<ObjectStorage> objectStorage;
  private final S3StorageProperties properties;
  private final PresignedUploadRepository presignedUploadRepository;

  public PresignedUrlService(
      Optional<ObjectStorage> objectStorage,
      S3StorageProperties properties,
      PresignedUploadRepository presignedUploadRepository) {
    this.objectStorage = objectStorage;
    this.properties = properties;
    this.presignedUploadRepository = presignedUploadRepository;
  }

  @Transactional
  public PresignedUploadUrl issueUpload(
      Long memberId,
      UploadResourceType resourceType,
      String objectKeyPrefix,
      String contentType,
      long contentLength) {
    validateFileSize(contentLength);
    ImageContentType imageContentType = resolveContentType(contentType);
    String objectKey =
        "%s%s.%s".formatted(objectKeyPrefix, UUID.randomUUID(), imageContentType.extension());

    PresignedUploadUrl result;
    try {
      result =
          getUploadStorage()
              .issuePutUrl(
                  objectKey,
                  imageContentType.mediaType(),
                  contentLength,
                  properties.presignedUrlExpiration());
    } catch (ObjectStorageException exception) {
      throw new PresignedUrlException(PresignedUrlException.Reason.STORAGE_ERROR, exception);
    }

    presignedUploadRepository.save(
        PresignedUpload.issue(
            memberId,
            resourceType,
            objectKey,
            imageContentType.mediaType(),
            contentLength,
            result.expiresAt()));
    return result;
  }

  @Transactional
  public String confirmUpload(
      Long memberId, UploadResourceType resourceType, String objectKey, String destinationKey) {
    PresignedUpload upload =
        presignedUploadRepository
            .findByObjectKeyForUpdate(objectKey)
            .orElseThrow(
                () ->
                    new PresignedUrlException(PresignedUrlException.Reason.UPLOAD_NOT_FOUND));
    validatePendingOwner(upload, memberId, resourceType);

    ObjectStorage storage = getUploadStorage();
    StoredObjectMetadata metadata = getMetadata(storage, objectKey);
    if (metadata.contentLength() != upload.getContentLength()
        || !upload.getContentType().equalsIgnoreCase(metadata.contentType())) {
      throw new PresignedUrlException(PresignedUrlException.Reason.METADATA_MISMATCH);
    }

    copy(storage, objectKey, destinationKey);
    upload.confirm();
    return destinationKey;
  }

  public PresignedDownloadUrl issueDownload(String objectKey) {
    try {
      return getDownloadStorage().issueGetUrl(objectKey, properties.presignedUrlExpiration());
    } catch (ObjectStorageException exception) {
      throw new PresignedUrlException(PresignedUrlException.Reason.STORAGE_ERROR, exception);
    }
  }

  private void validateFileSize(long contentLength) {
    if (contentLength <= 0 || contentLength > properties.maxUploadSize().toBytes()) {
      throw new PresignedUrlException(PresignedUrlException.Reason.INVALID_FILE_SIZE);
    }
  }

  private ImageContentType resolveContentType(String contentType) {
    try {
      return ImageContentType.from(contentType);
    } catch (UnsupportedImageContentTypeException exception) {
      throw new PresignedUrlException(
          PresignedUrlException.Reason.UNSUPPORTED_CONTENT_TYPE, exception);
    }
  }

  private void validatePendingOwner(
      PresignedUpload upload, Long memberId, UploadResourceType resourceType) {
    if (!Objects.equals(upload.getMemberId(), memberId)) {
      throw new PresignedUrlException(PresignedUrlException.Reason.ACCESS_DENIED);
    }
    if (upload.getResourceType() != resourceType || upload.getStatus() != UploadStatus.PENDING) {
      throw new PresignedUrlException(PresignedUrlException.Reason.INVALID_STATE);
    }
  }

  private ObjectStorage getUploadStorage() {
    return objectStorage.orElseThrow(
        () -> new PresignedUrlException(PresignedUrlException.Reason.STORAGE_DISABLED));
  }

  private ObjectStorage getDownloadStorage() {
    return objectStorage.orElseThrow(
        () -> new PresignedUrlException(PresignedUrlException.Reason.STORAGE_DISABLED));
  }

  private StoredObjectMetadata getMetadata(ObjectStorage storage, String objectKey) {
    try {
      return storage.getMetadata(objectKey);
    } catch (ObjectStorageException exception) {
      if (exception.getReason() == ObjectStorageException.Reason.OBJECT_NOT_FOUND) {
        throw new PresignedUrlException(PresignedUrlException.Reason.OBJECT_NOT_FOUND, exception);
      }
      throw new PresignedUrlException(PresignedUrlException.Reason.STORAGE_ERROR, exception);
    }
  }

  private void copy(ObjectStorage storage, String sourceKey, String destinationKey) {
    try {
      storage.copy(sourceKey, destinationKey);
    } catch (ObjectStorageException exception) {
      throw new PresignedUrlException(PresignedUrlException.Reason.STORAGE_ERROR, exception);
    }
  }
}
