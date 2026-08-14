package kr.lastdish.core.storage.application;

import java.util.Optional;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.common.api.exception.CommonErrorCode;
import kr.lastdish.core.common.exception.ErrorCode;
import kr.lastdish.core.storage.application.dto.PresignedUploadUrl;
import kr.lastdish.core.storage.infrastructure.S3StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ImageUploadService {

  private final Optional<PresignedUploadUrlProvider> presignedUploadUrlProvider;
  private final TemporaryImageKeyGenerator keyGenerator;
  private final S3StorageProperties properties;

  public PresignedUploadUrl issueDishUploadUrl(
      Long storeId, String contentType, long fileSize) {
    validateFileSize(fileSize);

    ImageContentType supportedContentType = ImageContentType.from(contentType);
    String objectKey = keyGenerator.generateDishKey(storeId, supportedContentType);
    PresignedUploadUrlProvider provider =
        presignedUploadUrlProvider.orElseThrow(
            () ->
                new BusinessException(
                    CommonErrorCode.SERVICE_UNAVAILABLE, "이미지 업로드 기능이 비활성화되어 있습니다."));
    return provider.issue(objectKey, supportedContentType.mediaType(), fileSize);
  }

  private void validateFileSize(long fileSize) {
    if (fileSize <= 0 || fileSize > properties.maxUploadSize().toBytes()) {
      throw new BusinessException(ErrorCode.INVALID_IMAGE_SIZE);
    }
  }
}
