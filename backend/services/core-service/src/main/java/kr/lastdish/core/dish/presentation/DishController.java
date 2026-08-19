package kr.lastdish.core.dish.presentation;

import jakarta.validation.Valid;
import java.util.List;
import kr.lastdish.common.api.response.ApiResponse;
import kr.lastdish.core.dish.application.DishFacade;
import kr.lastdish.core.dish.application.DishImageService;
import kr.lastdish.core.dish.application.DishService;
import kr.lastdish.core.dish.presentation.dto.DishCreateRequest;
import kr.lastdish.core.dish.presentation.dto.DishResponse;
import kr.lastdish.core.dish.presentation.dto.DishStatusRequest;
import kr.lastdish.core.dish.presentation.dto.DishUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/dishes")
public class DishController {
  private final DishFacade dishFacade;
  private final DishService dishService;
  private final DishImageService dishImageService;

  @PostMapping
  public ApiResponse<DishResponse> createDish(
      @RequestHeader("X-Authenticated-Member-Id") Long memberId,
      @Valid @RequestBody DishCreateRequest request) {
    return ApiResponse.ok(dishFacade.createDish(memberId, request));
  }

  @PutMapping("/{dishId}")
  public ApiResponse<DishResponse> updateDish(
      @RequestHeader("X-Authenticated-Member-Id") Long memberId,
      @PathVariable Long dishId,
      @Valid @RequestBody DishUpdateRequest request) {
    return ApiResponse.ok(dishFacade.updateDish(memberId, dishId, request));
  }

  @PatchMapping("/{dishId}")
  public ApiResponse<Void> deleteDish(
      @RequestHeader("X-Authenticated-Member-Id") Long memberId, @PathVariable Long dishId) {
    dishFacade.deleteDish(memberId, dishId);
    return ApiResponse.ok();
  }

  @GetMapping("/{dishId}")
  public ApiResponse<DishResponse> getEachDish(@PathVariable Long dishId) {
    DishResponse dish = dishService.getEachDish(dishId);
    return ApiResponse.ok(dishImageService.withDownloadUrl(dish));
  }

  @PatchMapping("/{dishId}/status")
  public ApiResponse<DishResponse> updateDishStatus(
      @RequestHeader("X-Authenticated-Member-Id") Long memberId,
      @PathVariable Long dishId,
      @Valid @RequestBody DishStatusRequest request) {
    return ApiResponse.ok(dishFacade.updateDishStatus(memberId, dishId, request));
  }

  @GetMapping()
  public ApiResponse<List<DishResponse>> getEachStoreDishes(@RequestParam Long storeId) {
    List<DishResponse> dishes =
        dishService.getOnSaleDishesByStoreId(storeId).stream()
            .map(dishImageService::withDownloadUrl)
            .toList();
    return ApiResponse.ok(dishes);
  }
}
