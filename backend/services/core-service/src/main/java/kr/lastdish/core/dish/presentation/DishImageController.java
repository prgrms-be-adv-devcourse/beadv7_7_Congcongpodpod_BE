package kr.lastdish.core.dish.presentation;

import jakarta.validation.Valid;
import kr.lastdish.common.api.response.ApiResponse;
import kr.lastdish.core.dish.application.DishImageService;
import kr.lastdish.core.dish.presentation.dto.DishImageDownloadUrlResponse;
import kr.lastdish.core.dish.presentation.dto.DishImageUploadUrlRequest;
import kr.lastdish.core.dish.presentation.dto.DishImageUploadUrlResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Dish 이미지 업로드용 Presigned PUT URL과 조회용 Presigned GET URL API를 제공합니다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/dishes")
public class DishImageController {

  private final DishImageService dishImageService;

  @PostMapping("/images/presigned-url")
  public ApiResponse<DishImageUploadUrlResponse> issueUploadUrl(
      @RequestHeader("X-Authenticated-Member-Id") Long memberId,
      @RequestHeader("X-Authenticated-Role") String role,
      @Valid @RequestBody DishImageUploadUrlRequest request) {
    return ApiResponse.ok(
        DishImageUploadUrlResponse.from(
            dishImageService.issue(
                memberId, role, request.storeId(), request.contentType(), request.fileSize())));
  }

  @GetMapping("/{dishId}/image/presigned-url")
  public ApiResponse<DishImageDownloadUrlResponse> issueDownloadUrl(@PathVariable Long dishId) {
    return ApiResponse.ok(
        DishImageDownloadUrlResponse.from(dishImageService.issueDownloadUrl(dishId)));
  }
}
