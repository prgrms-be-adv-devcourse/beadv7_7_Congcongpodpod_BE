package kr.lastdish.core.dish.presentation;

import jakarta.validation.Valid;
import kr.lastdish.common.api.response.ApiResponse;
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
  private final DishService dishService;

  @PostMapping
  public ApiResponse<DishResponse> createDish(@Valid @RequestBody DishCreateRequest request) {
    return ApiResponse.ok(dishService.createDish(request));
  }

  @PutMapping("/{dishId}")
  public ApiResponse<DishResponse> updateDish(
      @PathVariable Long dishId, @Valid @RequestBody DishUpdateRequest request) {
    return ApiResponse.ok(dishService.updateDish(dishId, request));
  }

  @PatchMapping("/{dishId}")
  public ApiResponse<Void> deleteDish(@PathVariable Long dishId) {
    dishService.deleteDish(dishId);
    return ApiResponse.ok();
  }

  @GetMapping("/{dishId}")
  public ApiResponse<DishResponse> getEachDish(@PathVariable Long dishId) {
    return ApiResponse.ok(dishService.getEachDish(dishId));
  }

  @PatchMapping("/{dishId}/status")
  public ApiResponse<DishResponse> updateDishStatus(
      @PathVariable Long dishId, @Valid @RequestBody DishStatusRequest request) {
    return ApiResponse.ok(dishService.updateDishStatus(dishId, request));
  }
}
