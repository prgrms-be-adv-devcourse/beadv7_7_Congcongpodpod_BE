package kr.lastdish.common.storage.application;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import kr.lastdish.common.storage.application.dto.PresignedDownloadUrl;
import kr.lastdish.common.storage.application.dto.PresignedUploadUrl;
import kr.lastdish.common.storage.application.dto.StoredObjectMetadata;
import kr.lastdish.common.storage.domain.PresignedUpload;
import kr.lastdish.common.storage.domain.PresignedUploadRepository;
import kr.lastdish.common.storage.domain.PresignedUrlException;
import kr.lastdish.common.storage.domain.UploadResourceType;
import kr.lastdish.common.storage.domain.UploadStatus;
import kr.lastdish.common.storage.image.ImageContentType;
import kr.lastdish.common.storage.image.UnsupportedImageContentTypeException;
import kr.lastdish.common.storage.infrastructure.s3.S3ObjectStorage;
import kr.lastdish.common.storage.infrastructure.s3.S3StorageException;
import kr.lastdish.common.storage.infrastructure.s3.S3StorageProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

/** Presigned 업로드 URL 발급·확정과 다운로드 URL 발급을 담당하는 공통 서비스입니다. */
@Slf4j
public class PresignedUrlService {

  private final Optional<S3ObjectStorage> s3ObjectStorage;
  private final S3StorageProperties properties;
  private final PresignedUploadRepository presignedUploadRepository;

  public PresignedUrlService(
      Optional<S3ObjectStorage> s3ObjectStorage,
      S3StorageProperties properties,
      PresignedUploadRepository presignedUploadRepository) {
    this.s3ObjectStorage = s3ObjectStorage;
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
          getStorage()
              .issuePutUrl(
                  objectKey, imageContentType.mediaType(), properties.presignedUrlExpiration());
    } catch (S3StorageException exception) {
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
                () -> new PresignedUrlException(PresignedUrlException.Reason.UPLOAD_NOT_FOUND));
    validatePendingOwner(upload, memberId, resourceType);

    S3ObjectStorage storage = getStorage();
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
      return getStorage().issueGetUrl(objectKey, properties.presignedUrlExpiration());
    } catch (S3StorageException exception) {
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
    if (upload.getResourceType() != resourceType
        || upload.getStatus() != UploadStatus.PENDING
        || !upload.getExpiresAt().isAfter(Instant.now())) {
      throw new PresignedUrlException(PresignedUrlException.Reason.INVALID_STATE);
    }
  }

  private S3ObjectStorage getStorage() {
    return s3ObjectStorage.orElseThrow(
        () -> new PresignedUrlException(PresignedUrlException.Reason.STORAGE_DISABLED));
  }

  private StoredObjectMetadata getMetadata(S3ObjectStorage storage, String objectKey) {
    try {
      return storage.getMetadata(objectKey);
    } catch (S3StorageException exception) {
      if (exception.getReason() == S3StorageException.Reason.OBJECT_NOT_FOUND) {
        throw new PresignedUrlException(PresignedUrlException.Reason.OBJECT_NOT_FOUND, exception);
      }
      throw new PresignedUrlException(PresignedUrlException.Reason.STORAGE_ERROR, exception);
    }
  }

  private void copy(S3ObjectStorage storage, String sourceKey, String destinationKey) {
    try {
      storage.copy(sourceKey, destinationKey);
    } catch (S3StorageException exception) {
      throw new PresignedUrlException(PresignedUrlException.Reason.STORAGE_ERROR, exception);
    }
  }
}
