package kr.lastdish.core.dish.application;

import java.util.Optional;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.common.api.exception.CommonErrorCode;
import kr.lastdish.common.storage.ObjectStorage;
import kr.lastdish.common.storage.ObjectStorageException;
import kr.lastdish.common.storage.PresignedDownloadUrl;
import kr.lastdish.common.storage.PresignedUploadUrl;
import kr.lastdish.common.storage.download.application.PresignedDownloadService;
import kr.lastdish.common.storage.download.domain.PresignedDownloadException;
import kr.lastdish.common.storage.upload.application.PresignedUploadService;
import kr.lastdish.common.storage.upload.domain.PresignedUploadException;
import kr.lastdish.common.storage.upload.domain.UploadResourceType;
import kr.lastdish.core.common.exception.ErrorCode;
import kr.lastdish.core.dish.presentation.dto.DishResponse;
import kr.lastdish.core.store.application.StoreFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DishImageService {

  private static final String SELLER_ROLE = "SELLER";

  private final StoreFacade storeFacade;
  private final PresignedUploadService presignedUploadService;
  private final PresignedDownloadService presignedDownloadService;
  private final DishService dishService;
  private final Optional<ObjectStorage> objectStorage;

  public PresignedUploadUrl issue(
      Long memberId, String role, Long storeId, String contentType, long fileSize) {

    // SELLER 검증
    if (!SELLER_ROLE.equals(role)) {
      throw new BusinessException(ErrorCode.IMAGE_UPLOAD_ACCESS_DENIED);
    }
    // 본인 매장 검증
    storeFacade.validateStoreOwner(storeId, memberId);
    return issueUploadUrl(memberId, storeId, contentType, fileSize);
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

  public PresignedDownloadUrl issueDownloadUrl(Long dishId) {
    String imageKey = dishService.getImageKey(dishId);
    return issueDownloadUrl(imageKey);
  }

  public DishResponse withDownloadUrl(DishResponse dishResponse) {
    PresignedDownloadUrl downloadUrl = issueDownloadUrl(dishResponse.thumbnailUrl());
    return dishResponse.withThumbnailUrl(downloadUrl.url().toExternalForm());
  }

  public void deleteImage(String objectKey) {
    if (objectKey == null || objectKey.isBlank()) {
      return;
    }
    if (!objectKey.startsWith("dish/")) {
      throw new BusinessException(ErrorCode.IMAGE_OBJECT_NOT_FOUND);
    }

    ObjectStorage storage =
        objectStorage.orElseThrow(
            () ->
                new BusinessException(
                    CommonErrorCode.SERVICE_UNAVAILABLE, "이미지 삭제 기능이 비활성화되어 있습니다."));
    try {
      storage.delete(objectKey);
    } catch (ObjectStorageException exception) {
      throw new BusinessException(ErrorCode.IMAGE_STORAGE_ERROR);
    }
  }

  private PresignedDownloadUrl issueDownloadUrl(String imageKey) {
    if (imageKey == null || imageKey.isBlank()) {
      throw new BusinessException(ErrorCode.IMAGE_OBJECT_NOT_FOUND);
    }
    try {
      return presignedDownloadService.issue(imageKey);
    } catch (PresignedDownloadException exception) {
      throw switch (exception.getReason()) {
        case STORAGE_DISABLED ->
            new BusinessException(CommonErrorCode.SERVICE_UNAVAILABLE, "이미지 조회 기능이 비활성화되어 있습니다.");
        case STORAGE_ERROR -> new BusinessException(ErrorCode.IMAGE_STORAGE_ERROR);
      };
    }
  }

  private PresignedUploadUrl issueUploadUrl(
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
