package kr.lastdish.core.store.presentation;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import kr.lastdish.core.common.response.ApiResponse;
import kr.lastdish.core.store.application.StoreService;
import kr.lastdish.core.store.application.dto.StorePageResult;
import kr.lastdish.core.store.application.dto.StoreResult;
import kr.lastdish.core.store.presentation.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/stores")
@RequiredArgsConstructor
public class StoreController {

  private final StoreService storeService;

  @PostMapping
  public ApiResponse<StoreResponse> registerStore(
      @RequestHeader("X-Member-Id") Long memberId, @Valid @RequestBody StoreCreateRequest request) {
    StoreResult result = storeService.register(request.toCommand(memberId));

    return ApiResponse.ok(StoreResponse.from(result));
  }

  @PutMapping("/{storeId}")
  public ApiResponse<StoreResponse> updateStore(
      @PathVariable Long storeId,
      @RequestHeader("X-Member-Id") Long memberId,
      @Valid @RequestBody UpdateStoreRequest request) {
    StoreResult result = storeService.update(storeId, memberId, request.toCommand());

    return ApiResponse.ok(StoreResponse.from(result));
  }

  @PatchMapping("/{storeId}/status")
  public ApiResponse<StoreResponse> changeStatus(
      @PathVariable Long storeId,
      @RequestHeader("X-Member-Id") Long memberId,
      @Valid @RequestBody ChangeStoreStatusRequest request) {
    StoreResult result = storeService.changeStatus(storeId, memberId, request.status());

    return ApiResponse.ok(StoreResponse.from(result));
  }

  @PatchMapping("/{storeId}/delete")
  public ApiResponse<Void> deleteStore(
      @PathVariable Long storeId, @RequestHeader("X-Member-Id") Long memberId) {
    storeService.deleteStore(storeId, memberId);

    return ApiResponse.ok();
  }

  @GetMapping("/{storeId}")
  public ApiResponse<StoreResponse> getStore(@PathVariable Long storeId) {
    StoreResult result = storeService.getStore(storeId);

    return ApiResponse.ok(StoreResponse.from(result));
  }

  @GetMapping("/nearby")
  public ApiResponse<StoreSearchResponse> getNearbyStores(
      @RequestParam BigDecimal latitude,
      @RequestParam BigDecimal longitude,
      @RequestParam(defaultValue = "3") double radiusKm,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {

    StorePageResult result =
        storeService.getNearbyStores(latitude, longitude, radiusKm, page, size);

    return ApiResponse.ok(StoreSearchResponse.from(result));
  }
}
