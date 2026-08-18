package kr.lastdish.core.dish.application;

import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.common.api.exception.CommonErrorCode;
import kr.lastdish.common.storage.PresignedUploadUrl;
import kr.lastdish.common.storage.upload.application.PresignedUploadService;
import kr.lastdish.common.storage.upload.domain.PresignedUploadException;
import kr.lastdish.common.storage.upload.domain.UploadResourceType;
import kr.lastdish.core.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DishImageUploadService {

  private final PresignedUploadService presignedUploadService;

  public PresignedUploadUrl issueUploadUrl(
      Long memberId, Long storeId, String contentType, long fileSize) {
    try {
      return presignedUploadService.issue(
          memberId,
          UploadResourceType.DISH,
          "tmp/dish/%d/".formatted(storeId),
          contentType,
          fileSize);
    } catch (PresignedUploadException exception) {
      throw toBusinessException(exception);
    }
  }

  public String confirmUpload(Long memberId, Long storeId, String objectKey) {
    String expectedPrefix = "tmp/dish/%d/".formatted(storeId);
    if (objectKey == null || !objectKey.startsWith(expectedPrefix)) {
      throw new BusinessException(ErrorCode.IMAGE_UPLOAD_ACCESS_DENIED);
    }

    String finalKey = objectKey.substring("tmp/".length());
    try {
      return presignedUploadService.confirm(memberId, UploadResourceType.DISH, objectKey, finalKey);
    } catch (PresignedUploadException exception) {
      throw toBusinessException(exception);
    }
  }

  private BusinessException toBusinessException(PresignedUploadException exception) {
    return switch (exception.getReason()) {
      case STORAGE_DISABLED ->
          new BusinessException(CommonErrorCode.SERVICE_UNAVAILABLE, "이미지 업로드 기능이 비활성화되어 있습니다.");
      case INVALID_FILE_SIZE -> new BusinessException(ErrorCode.INVALID_IMAGE_SIZE);
      case UNSUPPORTED_CONTENT_TYPE -> new BusinessException(ErrorCode.UNSUPPORTED_IMAGE_TYPE);
      case UPLOAD_NOT_FOUND -> new BusinessException(ErrorCode.PRESIGNED_UPLOAD_NOT_FOUND);
      case ACCESS_DENIED -> new BusinessException(ErrorCode.IMAGE_UPLOAD_ACCESS_DENIED);
      case INVALID_STATE -> new BusinessException(ErrorCode.PRESIGNED_UPLOAD_INVALID_STATE);
      case METADATA_MISMATCH -> new BusinessException(ErrorCode.IMAGE_METADATA_MISMATCH);
      case OBJECT_NOT_FOUND -> new BusinessException(ErrorCode.IMAGE_OBJECT_NOT_FOUND);
      case STORAGE_ERROR -> new BusinessException(ErrorCode.IMAGE_STORAGE_ERROR);
    };
  }
}
