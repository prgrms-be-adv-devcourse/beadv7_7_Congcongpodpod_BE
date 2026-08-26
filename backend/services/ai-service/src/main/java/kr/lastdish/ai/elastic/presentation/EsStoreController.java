package kr.lastdish.ai.elastic.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import kr.lastdish.ai.elastic.application.StoreQueryService;
import kr.lastdish.ai.elastic.presentation.dto.StoreResponse;
import kr.lastdish.common.api.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "AI Store Query API", description = "ES 기반 매장 및 대표 메뉴 목록 조회 API")
@RestController
@RequestMapping("/api/v1/ai/stores")
@RequiredArgsConstructor
public class EsStoreController {

  private final StoreQueryService storeQueryService;

  @Operation(summary = "반경 내 매장 목록 및 대표 메뉴 조회")
  @GetMapping("/nearby")
  public ResponseEntity<ApiResponse<List<StoreResponse>>> getNearbyStores(
      @RequestParam Double latitude,
      @RequestParam Double longitude,
      @RequestParam(defaultValue = "3.0") Double radiusKm,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {

    List<StoreResponse> stores =
        storeQueryService.getStoresByLocation(latitude, longitude, radiusKm, page, size);

    return ResponseEntity.ok(ApiResponse.ok(stores));
  }
}
