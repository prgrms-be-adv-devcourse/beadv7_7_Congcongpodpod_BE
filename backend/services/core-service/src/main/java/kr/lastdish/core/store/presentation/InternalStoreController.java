package kr.lastdish.core.store.presentation;

import java.time.Instant;
import java.util.List;
import kr.lastdish.common.api.response.ApiResponse;
import kr.lastdish.core.store.application.StoreFacade;
import kr.lastdish.core.store.presentation.dto.InternalStoreResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/stores")
@RequiredArgsConstructor
public class InternalStoreController {
  private final StoreFacade storeFacade;

  // 검색 전용 내부 API, 해당 매장의 매장 정보와 상품 정보를 합쳐 완성된 검색 문서를 반환
  @GetMapping("/{storeId}/renewal")
  public ApiResponse<InternalStoreResponse> getSearchDoc(@PathVariable Long storeId) {
    return ApiResponse.ok(
        InternalStoreResponse.from(storeFacade.getDishAndStoreByStoreIdForRenewal(storeId)));
  }

  @GetMapping("/renewal")
  public ApiResponse<List<InternalStoreResponse>> getSearchDocs(
      @RequestParam Instant from, @RequestParam Instant to) {
    return ApiResponse.ok(
        storeFacade.getDishAndStoresForRenewal(from, to).stream()
            .map(InternalStoreResponse::from)
            .toList());
  }
}
