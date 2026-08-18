package kr.lastdish.common.storage.upload.application;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import kr.lastdish.common.storage.ObjectStorage;
import kr.lastdish.common.storage.ObjectStorageException;
import kr.lastdish.common.storage.PresignedUploadUrl;
import kr.lastdish.common.storage.StoredObjectMetadata;
import kr.lastdish.common.storage.image.ImageContentType;
import kr.lastdish.common.storage.image.UnsupportedImageContentTypeException;
import kr.lastdish.common.storage.s3.S3StorageProperties;
import kr.lastdish.common.storage.upload.domain.PresignedUpload;
import kr.lastdish.common.storage.upload.domain.PresignedUploadException;
import kr.lastdish.common.storage.upload.domain.PresignedUploadRepository;
import kr.lastdish.common.storage.upload.domain.UploadResourceType;
import kr.lastdish.common.storage.upload.domain.UploadStatus;
import org.springframework.transaction.annotation.Transactional;

public class PresignedUploadService {

  private final Optional<ObjectStorage> objectStorage;
  private final S3StorageProperties properties;
  private final PresignedUploadRepository presignedUploadRepository;

  public PresignedUploadService(
      Optional<ObjectStorage> objectStorage,
      S3StorageProperties properties,
      PresignedUploadRepository presignedUploadRepository) {
    this.objectStorage = objectStorage;
    this.properties = properties;
    this.presignedUploadRepository = presignedUploadRepository;
  }

  @Transactional
  public PresignedUploadUrl issue(
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
          getObjectStorage()
              .issuePutUrl(
                  objectKey,
                  imageContentType.mediaType(),
                  contentLength,
                  properties.presignedUrlExpiration());
    } catch (ObjectStorageException exception) {
      throw new PresignedUploadException(PresignedUploadException.Reason.STORAGE_ERROR, exception);
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
  public String confirm(
      Long memberId, UploadResourceType resourceType, String objectKey, String destinationKey) {
    PresignedUpload upload =
        presignedUploadRepository
            .findByObjectKeyForUpdate(objectKey)
            .orElseThrow(
                () ->
                    new PresignedUploadException(PresignedUploadException.Reason.UPLOAD_NOT_FOUND));
    validatePendingOwner(upload, memberId, resourceType);

    ObjectStorage storage = getObjectStorage();
    StoredObjectMetadata metadata = getMetadata(storage, objectKey);
    if (metadata.contentLength() != upload.getContentLength()
        || !upload.getContentType().equalsIgnoreCase(metadata.contentType())) {
      throw new PresignedUploadException(PresignedUploadException.Reason.METADATA_MISMATCH);
    }

    copy(storage, objectKey, destinationKey);
    upload.confirm();
    presignedUploadRepository.save(upload);
    return destinationKey;
  }

  private void validateFileSize(long contentLength) {
    if (contentLength <= 0 || contentLength > properties.maxUploadSize().toBytes()) {
      throw new PresignedUploadException(PresignedUploadException.Reason.INVALID_FILE_SIZE);
    }
  }

  private ImageContentType resolveContentType(String contentType) {
    try {
      return ImageContentType.from(contentType);
    } catch (UnsupportedImageContentTypeException exception) {
      throw new PresignedUploadException(
          PresignedUploadException.Reason.UNSUPPORTED_CONTENT_TYPE, exception);
    }
  }

  private void validatePendingOwner(
      PresignedUpload upload, Long memberId, UploadResourceType resourceType) {
    if (!Objects.equals(upload.getMemberId(), memberId)) {
      throw new PresignedUploadException(PresignedUploadException.Reason.ACCESS_DENIED);
    }
    if (upload.getResourceType() != resourceType || upload.getStatus() != UploadStatus.PENDING) {
      throw new PresignedUploadException(PresignedUploadException.Reason.INVALID_STATE);
    }
  }

  private ObjectStorage getObjectStorage() {
    return objectStorage.orElseThrow(
        () -> new PresignedUploadException(PresignedUploadException.Reason.STORAGE_DISABLED));
  }

  private StoredObjectMetadata getMetadata(ObjectStorage storage, String objectKey) {
    try {
      return storage.getMetadata(objectKey);
    } catch (ObjectStorageException exception) {
      if (exception.getReason() == ObjectStorageException.Reason.OBJECT_NOT_FOUND) {
        throw new PresignedUploadException(
            PresignedUploadException.Reason.OBJECT_NOT_FOUND, exception);
      }
      throw new PresignedUploadException(PresignedUploadException.Reason.STORAGE_ERROR, exception);
    }
  }

  private void copy(ObjectStorage storage, String sourceKey, String destinationKey) {
    try {
      storage.copy(sourceKey, destinationKey);
    } catch (ObjectStorageException exception) {
      throw new PresignedUploadException(PresignedUploadException.Reason.STORAGE_ERROR, exception);
    }
  }
}
