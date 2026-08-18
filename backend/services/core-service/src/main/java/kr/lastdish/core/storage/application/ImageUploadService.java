package kr.lastdish.core.storage.application;

import java.util.Optional;
import java.util.UUID;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.common.api.exception.CommonErrorCode;
import kr.lastdish.core.common.exception.ErrorCode;
import kr.lastdish.core.storage.application.dto.PresignedUploadUrl;
import kr.lastdish.core.storage.domain.PresignedUpload;
import kr.lastdish.core.storage.domain.PresignedUploadRepository;
import kr.lastdish.core.storage.domain.UploadResourceType;
import kr.lastdish.core.storage.infrastructure.S3PresignedUploadUrlProvider;
import kr.lastdish.core.storage.infrastructure.S3StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
@RequiredArgsConstructor
public class ImageUploadService {

  private final Optional<S3PresignedUploadUrlProvider> presignedUploadUrlProvider;
  private final S3StorageProperties properties;
  private final PresignedUploadRepository presignedUploadRepository;
  // S3를 사용하지 않는 로컬·테스트 환경에서도 애플리케이션을 실행할 수 있도록 선택적으로 주입한다.
  private final Optional<S3Client> s3Client;

  @Transactional
  public PresignedUploadUrl issueDishUploadUrl(
      Long memberId, Long storeId, String contentType, long fileSize) {
    validateFileSize(fileSize);

    // objectKey 발급
    ImageContentType supportedContentType = ImageContentType.from(contentType);
    String objectKey = issueDishObjectKey(storeId, supportedContentType);

    // Presigned Url 발급
    S3PresignedUploadUrlProvider provider =
        presignedUploadUrlProvider.orElseThrow(
            () ->
                new BusinessException(
                    CommonErrorCode.SERVICE_UNAVAILABLE, "이미지 업로드 기능이 비활성화되어 있습니다."));
    PresignedUploadUrl result =
        provider.issue(objectKey, supportedContentType.mediaType(), fileSize);

    // URL 발급 이력 저장
    presignedUploadRepository.save(
        PresignedUpload.issue(
            memberId,
            UploadResourceType.DISH,
            objectKey,
            supportedContentType.mediaType(),
            fileSize,
            result.expiresAt()));
    return result;
  }

  @Transactional
  public String confirmDishUpload(Long memberId, Long storeId, String objectKey) {
    String expectedPrefix = "tmp/dish/%d/".formatted(storeId);
    if (objectKey == null || !objectKey.startsWith(expectedPrefix)) {
      throw new BusinessException(ErrorCode.IMAGE_UPLOAD_ACCESS_DENIED);
    }

    PresignedUpload upload =
        presignedUploadRepository
            .findByObjectKeyForUpdate(objectKey)
            .orElseThrow(() -> new BusinessException(ErrorCode.PRESIGNED_UPLOAD_NOT_FOUND));
    upload.validatePendingOwner(memberId, UploadResourceType.DISH);

    S3Client client =
        s3Client.orElseThrow(
            () ->
                new BusinessException(
                    CommonErrorCode.SERVICE_UNAVAILABLE, "이미지 업로드 기능이 비활성화되어 있습니다."));

    HeadObjectResponse object = headObject(client, objectKey);
    if (object.contentLength() != upload.getContentLength()
        || !upload.getContentType().equalsIgnoreCase(object.contentType())) {
      throw new BusinessException(ErrorCode.IMAGE_METADATA_MISMATCH);
    }

    String finalKey = objectKey.substring("tmp/".length());
    copyObject(client, objectKey, finalKey);
    upload.confirm();
    presignedUploadRepository.save(upload);
    return finalKey;
  }

  private HeadObjectResponse headObject(S3Client client, String objectKey) {
    try {
      return client.headObject(
          HeadObjectRequest.builder().bucket(properties.bucket()).key(objectKey).build());
    } catch (S3Exception exception) {
      if (exception.statusCode() == 404) {
        throw new BusinessException(ErrorCode.IMAGE_OBJECT_NOT_FOUND);
      }
      throw new BusinessException(ErrorCode.IMAGE_STORAGE_ERROR);
    }
  }

  private void copyObject(S3Client client, String sourceKey, String destinationKey) {
    try {
      client.copyObject(
          CopyObjectRequest.builder()
              .copySource(properties.bucket() + "/" + sourceKey)
              .destinationBucket(properties.bucket())
              .destinationKey(destinationKey)
              .build());
    } catch (S3Exception exception) {
      throw new BusinessException(ErrorCode.IMAGE_STORAGE_ERROR);
    }
  }

  private String issueDishObjectKey(Long storeId, ImageContentType supportedContentType) {
    return "tmp/dish/%d/%s.%s"
        .formatted(storeId, UUID.randomUUID(), supportedContentType.extension());
  }

  private void validateFileSize(long fileSize) {
    if (fileSize <= 0 || fileSize > properties.maxUploadSize().toBytes()) {
      throw new BusinessException(ErrorCode.INVALID_IMAGE_SIZE);
    }
  }
}
