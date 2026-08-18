package kr.lastdish.core.dish.application;

import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.common.storage.PresignedUploadUrl;
import kr.lastdish.core.common.exception.ErrorCode;
import kr.lastdish.core.store.application.StoreFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DishImageService {

  private static final String SELLER_ROLE = "SELLER";

  private final StoreFacade storeFacade;
  private final DishImageUploadService dishImageUploadService;

  public PresignedUploadUrl issue(
      Long memberId, String role, Long storeId, String contentType, long fileSize) {

    // SELLER 검증
    if (!SELLER_ROLE.equals(role)) {
      throw new BusinessException(ErrorCode.IMAGE_UPLOAD_ACCESS_DENIED);
    }
    // 본인 매장 검즐
    storeFacade.validateStoreOwner(storeId, memberId);
    return dishImageUploadService.issueUploadUrl(memberId, storeId, contentType, fileSize);
  }
}
