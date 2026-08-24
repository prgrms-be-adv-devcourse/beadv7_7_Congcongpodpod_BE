package kr.lastdish.core.favorite.presentation;

import jakarta.validation.Valid;
import java.util.List;
import kr.lastdish.common.api.response.ApiResponse;
import kr.lastdish.core.favorite.application.StoreFavoriteService;
import kr.lastdish.core.favorite.presentation.dto.FavoriteAddRequest;
import kr.lastdish.core.favorite.presentation.dto.FavoriteStatusResponse;
import kr.lastdish.core.favorite.presentation.dto.FavoriteStoreResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/favorites")
@RequiredArgsConstructor
public class FavoriteController {

  private final StoreFavoriteService storeFavoriteService;

  @PostMapping
  public ApiResponse<Void> addFavorite(
      @RequestHeader("X-Authenticated-Member-Id") Long memberId,
      @Valid @RequestBody FavoriteAddRequest request) {
    storeFavoriteService.addFavorite(memberId, request.storeId());
    return ApiResponse.ok();
  }

  @DeleteMapping("/{storeId}")
  public ResponseEntity<Void> removeFavorite(
      @RequestHeader("X-Authenticated-Member-Id") Long memberId, @PathVariable Long storeId) {
    storeFavoriteService.removeFavorite(memberId, storeId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping
  public ApiResponse<List<FavoriteStoreResponse>> getFavorites(
      @RequestHeader("X-Authenticated-Member-Id") Long memberId) {
    List<FavoriteStoreResponse> stores =
        storeFavoriteService.getFavorites(memberId).stream()
            .map(FavoriteStoreResponse::from)
            .toList();
    return ApiResponse.ok(stores);
  }

  @GetMapping("/{storeId}")
  public ApiResponse<FavoriteStatusResponse> getFavoriteStatus(
      @RequestHeader("X-Authenticated-Member-Id") Long memberId, @PathVariable Long storeId) {
    return ApiResponse.ok(
        new FavoriteStatusResponse(storeFavoriteService.isFavorite(memberId, storeId)));
  }
}
