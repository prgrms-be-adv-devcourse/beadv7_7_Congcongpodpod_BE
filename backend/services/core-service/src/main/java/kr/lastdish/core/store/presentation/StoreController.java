package kr.lastdish.core.store.presentation;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import kr.lastdish.common.api.response.ApiResponse;
import kr.lastdish.core.dish.application.DishImageService;
import kr.lastdish.core.dish.presentation.dto.DishResponse;
import kr.lastdish.core.store.application.StoreFacade;
import kr.lastdish.core.store.application.StoreService;
import kr.lastdish.core.store.application.dto.PayoutAccountResult;
import kr.lastdish.core.store.application.dto.StorePageResult;
import kr.lastdish.core.store.application.dto.StoreResult;
import kr.lastdish.core.store.domain.Category;
import kr.lastdish.core.store.presentation.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stores")
@RequiredArgsConstructor
public class StoreController {

  private final StoreService storeService;
  private final StoreFacade storeFacade;
  private final DishImageService dishImageService;

  @PostMapping
  public ApiResponse<StoreResponse> registerStore(
      @RequestHeader("X-Authenticated-Member-Id") Long memberId,
      @Valid @RequestBody StoreCreateRequest request) {
    StoreResult result = storeService.register(request.toCommand(memberId));

    return ApiResponse.ok(StoreResponse.from(result));
  }

  @PutMapping("/{storeId}")
  public ApiResponse<StoreResponse> updateStore(
      @PathVariable Long storeId,
      @RequestHeader("X-Authenticated-Member-Id") Long memberId,
      @Valid @RequestBody UpdateStoreRequest request) {
    StoreResult result = storeService.update(storeId, memberId, request.toCommand());

    return ApiResponse.ok(StoreResponse.from(result));
  }

  @PatchMapping("/{storeId}/status")
  public ApiResponse<StoreResponse> changeStatus(
      @PathVariable Long storeId,
      @RequestHeader("X-Authenticated-Member-Id") Long memberId,
      @Valid @RequestBody ChangeStoreStatusRequest request) {
    StoreResult result = storeService.changeStatus(storeId, memberId, request.status());

    return ApiResponse.ok(StoreResponse.from(result));
  }

  @PatchMapping("/{storeId}/delete")
  public ApiResponse<Void> deleteStore(
      @PathVariable Long storeId, @RequestHeader("X-Authenticated-Member-Id") Long memberId) {
    storeService.deleteStore(storeId, memberId);

    return ApiResponse.ok();
  }

  @GetMapping("/{storeId}")
  public ApiResponse<StoreResponse> getStore(@PathVariable Long storeId) {
    StoreResult result = storeService.getStore(storeId);

    return ApiResponse.ok(StoreResponse.from(result));
  }

  @GetMapping("/mine")
  public ApiResponse<List<StoreResponse>> getMyStores(
      @RequestHeader("X-Authenticated-Member-Id") Long memberId) {
    List<StoreResponse> result =
        storeService.getMyStores(memberId).stream().map(StoreResponse::from).toList();

    return ApiResponse.ok(result);
  }

  @GetMapping("/{storeId}/dish")
  public ApiResponse<DishResponse> getMyDish(
      @PathVariable Long storeId, @RequestHeader("X-Authenticated-Member-Id") Long memberId) {
    return ApiResponse.ok(storeFacade.getMyDish(storeId, memberId));
  }

  @GetMapping("/{storeId}/dishes")
  public ApiResponse<List<DishResponse>> getMyDishes(
      @PathVariable Long storeId, @RequestHeader("X-Authenticated-Member-Id") Long memberId) {
    List<DishResponse> result =
        storeFacade.getMyDishes(storeId, memberId).stream()
            .map(dishImageService::withDownloadUrl)
            .toList();
    return ApiResponse.ok(result);
  }

  @GetMapping("/nearby")
  public ApiResponse<StoreSearchResponse> getNearbyStores(
      @RequestParam BigDecimal latitude,
      @RequestParam BigDecimal longitude,
      @RequestParam(required = false) Category category,
      @RequestParam(defaultValue = "3") double radiusKm,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {

    StorePageResult result =
        storeFacade.getNearbyStores(latitude, longitude, category, radiusKm, page, size);

    return ApiResponse.ok(StoreSearchResponse.from(result));
  }

  // 매장 정산 계좌
  @PostMapping("/{storeId}/payoutAccount")
  public ApiResponse<StoreAccountResponse> registerPayoutAccount(
      @PathVariable Long storeId,
      @RequestHeader("X-Authenticated-Member-Id") Long memberId,
      @Valid @RequestBody StoreAccountRequest request) {
    PayoutAccountResult result =
        storeService.registerPayoutAccount(
            storeId,
            memberId,
            request.bankName(),
            request.accountNumber(),
            request.accountHolder());

    return ApiResponse.ok(StoreAccountResponse.from(result));
  }

  @PutMapping("/{storeId}/payoutAccount")
  public ApiResponse<StoreAccountResponse> updatePayoutAccount(
      @PathVariable Long storeId,
      @RequestHeader("X-Authenticated-Member-Id") Long memberId,
      @Valid @RequestBody StoreAccountRequest request) {
    PayoutAccountResult result =
        storeService.updatePayoutAccount(
            storeId,
            memberId,
            request.bankName(),
            request.accountNumber(),
            request.accountHolder());

    return ApiResponse.ok(StoreAccountResponse.from(result));
  }
}
