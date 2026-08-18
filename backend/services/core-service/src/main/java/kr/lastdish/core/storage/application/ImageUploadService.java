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

@Service
@RequiredArgsConstructor
public class ImageUploadService {

  private final Optional<S3PresignedUploadUrlProvider> presignedUploadUrlProvider;
  private final S3StorageProperties properties;
  private final PresignedUploadRepository presignedUploadRepository;

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
