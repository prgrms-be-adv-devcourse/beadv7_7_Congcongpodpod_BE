package kr.lastdish.ai.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import kr.lastdish.ai.application.StoreQueryService;
import kr.lastdish.ai.presentation.dto.StoreResponse;
import kr.lastdish.common.api.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "AI Store Query API", description = "ES 기반 매장 및 대표 메뉴 목록 조회 API")
@RestController
@RequestMapping("/api/v1/ai/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreQueryService storeQueryService;

    @Operation(summary = "매장 목록 및 대표(최저가) 메뉴 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<StoreResponse>>> getStores(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<StoreResponse> stores = storeQueryService.getAllStores(page, size);
        return ResponseEntity.ok(ApiResponse.ok(stores));
    }
}