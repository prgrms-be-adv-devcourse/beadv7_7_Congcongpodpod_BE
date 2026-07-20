package kr.lastdish.core.store.presentation;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import kr.lastdish.core.store.application.StoreService;
import kr.lastdish.core.store.application.dto.StorePageResult;
import kr.lastdish.core.store.application.dto.StoreResult;
import kr.lastdish.core.store.presentation.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/stores")
@RequiredArgsConstructor
public class StoreController {

  private final StoreService storeService;

  @PostMapping
  public ResponseEntity<StoreResponse> registerStore(
      @RequestHeader("X-Member-Id") Long memberId, @Valid @RequestBody StoreCreateRequest request) {
    StoreResult result = storeService.register(request.toCommand(memberId));

    return ResponseEntity.status(HttpStatus.CREATED).body(StoreResponse.from(result));
  }

  @PutMapping("/{storeId}")
  public ResponseEntity<StoreResponse> updateStore(
      @PathVariable Long storeId,
      @RequestHeader("X-Member-Id") Long memberId,
      @Valid @RequestBody UpdateStoreRequest request) {
    StoreResult result = storeService.update(storeId, memberId, request.toCommand());

    return ResponseEntity.ok(StoreResponse.from(result));
  }

  @PatchMapping("/{storeId}/status")
  public ResponseEntity<StoreResponse> changeStatus(
      @PathVariable Long storeId,
      @RequestHeader("X-Member-Id") Long memberId,
      @Valid @RequestBody ChangeStoreStatusRequest request) {
    StoreResult result = storeService.changeStatus(storeId, memberId, request.status());

    return ResponseEntity.ok(StoreResponse.from(result));
  }

  @PatchMapping("/{storeId}/delete")
  public ResponseEntity<Void> deleteStore(
      @PathVariable Long storeId, @RequestHeader("X-Member-Id") Long memberId) {
    storeService.deleteStore(storeId, memberId);

    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{storeId}")
  public ResponseEntity<StoreResponse> getStore(@PathVariable Long storeId) {
    StoreResult result = storeService.getStore(storeId);

    return ResponseEntity.ok(StoreResponse.from(result));
  }

  @GetMapping("/nearby")
  public ResponseEntity<StoreSearchResponse> getNearbyStores(
      @RequestParam BigDecimal latitude,
      @RequestParam BigDecimal longitude,
      @RequestParam(defaultValue = "3") double radiusKm,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {

    StorePageResult result =
        storeService.getNearbyStores(latitude, longitude, radiusKm, page, size);

    return ResponseEntity.ok(StoreSearchResponse.from(result));
  }
}
