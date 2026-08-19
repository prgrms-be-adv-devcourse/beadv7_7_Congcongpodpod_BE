package kr.lastdish.core.dish.application;

import java.util.Optional;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.common.api.exception.CommonErrorCode;
import kr.lastdish.common.storage.application.PresignedUrlService;
import kr.lastdish.common.storage.application.dto.PresignedDownloadUrl;
import kr.lastdish.common.storage.application.dto.PresignedUploadUrl;
import kr.lastdish.common.storage.domain.PresignedUrlException;
import kr.lastdish.common.storage.domain.UploadResourceType;
import kr.lastdish.common.storage.infrastructure.s3.S3ObjectStorage;
import kr.lastdish.common.storage.infrastructure.s3.S3StorageException;
import kr.lastdish.core.common.exception.ErrorCode;
import kr.lastdish.core.dish.presentation.dto.DishResponse;
import kr.lastdish.core.store.application.StoreFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 공통 스토리지 기능을 Dish 정책에 맞게 연결하는 애플리케이션 서비스입니다.
 *
 * <p>SELLER·매장 소유권 검증, Dish 전용 Key 검증, 업로드 확정, 조회 URL 발급과 이미지 삭제 오류 변환을 담당합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DishImageService {

  private static final String SELLER_ROLE = "SELLER";

  private final StoreFacade storeFacade;
  private final PresignedUrlService presignedUrlService;
  private final DishService dishService;
  private final Optional<S3ObjectStorage> s3ObjectStorage;

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
    String finalKey = resolveFinalKey(storeId, objectKey);
    try {
      return presignedUrlService.confirmUpload(
          memberId, UploadResourceType.DISH, objectKey, finalKey);
    } catch (PresignedUrlException exception) {
      throw toBusinessException(exception);
    }
  }

  static String resolveFinalKey(Long storeId, String objectKey) {
    String expectedPrefix = "tmp/dish/%d/".formatted(storeId);
    if (objectKey == null || !objectKey.startsWith(expectedPrefix)) {
      throw new BusinessException(ErrorCode.IMAGE_UPLOAD_ACCESS_DENIED);
    }
    return objectKey.substring("tmp/".length());
  }

  public PresignedDownloadUrl issueDownloadUrl(Long dishId) {
    String imageKey = dishService.getImageKey(dishId);
    return issueDownloadUrl(imageKey);
  }

  public DishResponse withDownloadUrl(DishResponse dishResponse) {
    String imageKey = dishResponse.thumbnailUrl();
    try {
      PresignedDownloadUrl downloadUrl = presignedUrlService.issueDownload(imageKey);
      return dishResponse.withThumbnailUrl(downloadUrl.url().toExternalForm());
    } catch (PresignedUrlException exception) {
      log.warn("Dish 이미지 조회 URL 발급에 실패했습니다. imageKey={}", imageKey, exception);
      return dishResponse.withThumbnailUrl(null);
    }
  }

  public void deleteImage(String objectKey) {
    if (objectKey == null || objectKey.isBlank()) {
      return;
    }
    if (!objectKey.startsWith("dish/")) {
      throw new BusinessException(ErrorCode.IMAGE_OBJECT_NOT_FOUND);
    }

    S3ObjectStorage storage =
        s3ObjectStorage.orElseThrow(
            () ->
                new BusinessException(
                    CommonErrorCode.SERVICE_UNAVAILABLE, "이미지 삭제 기능이 비활성화되어 있습니다."));
    try {
      storage.delete(objectKey);
    } catch (S3StorageException exception) {
      throw new BusinessException(ErrorCode.IMAGE_STORAGE_ERROR);
    }
  }

  public void deleteImageSafely(String objectKey) {
    try {
      deleteImage(objectKey);
    } catch (RuntimeException exception) {
      log.error("Dish 이미지 삭제에 실패했습니다. 수동 정리가 필요합니다. objectKey={}", objectKey, exception);
    }
  }

  private PresignedDownloadUrl issueDownloadUrl(String imageKey) {
    if (imageKey == null || imageKey.isBlank()) {
      throw new BusinessException(ErrorCode.IMAGE_OBJECT_NOT_FOUND);
    }
    try {
      return presignedUrlService.issueDownload(imageKey);
    } catch (PresignedUrlException exception) {
      throw toBusinessException(exception);
    }
  }

  private PresignedUploadUrl issueUploadUrl(
      Long memberId, Long storeId, String contentType, long fileSize) {
    try {
      return presignedUrlService.issueUpload(
          memberId,
          UploadResourceType.DISH,
          "tmp/dish/%d/".formatted(storeId),
          contentType,
          fileSize);
    } catch (PresignedUrlException exception) {
      throw toBusinessException(exception);
    }
  }

  private BusinessException toBusinessException(PresignedUrlException exception) {
    return switch (exception.getReason()) {
      case STORAGE_DISABLED ->
          new BusinessException(CommonErrorCode.SERVICE_UNAVAILABLE, "이미지 저장소 기능이 비활성화되어 있습니다.");
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
