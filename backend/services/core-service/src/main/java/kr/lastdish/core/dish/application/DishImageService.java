package kr.lastdish.core.dish.application;

import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.core.common.exception.ErrorCode;
import kr.lastdish.core.storage.application.ImageUploadService;
import kr.lastdish.core.storage.application.dto.PresignedUploadUrl;
import kr.lastdish.core.store.application.StoreFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DishImageService {

  private static final String SELLER_ROLE = "SELLER";

  private final StoreFacade storeFacade;
  private final ImageUploadService imageUploadService;

  public PresignedUploadUrl issue(
      Long memberId,
      String role,
      Long storeId,
      String contentType,
      long fileSize) {
    if (!SELLER_ROLE.equals(role)) {
      throw new BusinessException(ErrorCode.IMAGE_UPLOAD_ACCESS_DENIED);
    }

    storeFacade.validateStoreOwner(storeId, memberId);
    return imageUploadService.issueDishUploadUrl(storeId, contentType, fileSize);
  }
}
