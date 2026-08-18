package kr.lastdish.core.dish.presentation;

import jakarta.validation.Valid;
import kr.lastdish.common.api.response.ApiResponse;
import kr.lastdish.core.dish.application.DishImageService;
import kr.lastdish.core.dish.presentation.dto.DishImageUploadUrlRequest;
import kr.lastdish.core.dish.presentation.dto.DishImageUploadUrlResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/dishes/images")
public class DishImageController {

  private final DishImageService dishImageService;

  @PostMapping("/presigned-url")
  public ApiResponse<DishImageUploadUrlResponse> issueUploadUrl(
      @RequestHeader("X-Authenticated-Member-Id") Long memberId,
      @RequestHeader("X-Authenticated-Role") String role,
      @Valid @RequestBody DishImageUploadUrlRequest request) {
    return ApiResponse.ok(
        DishImageUploadUrlResponse.from(
            dishImageService.issue(
                memberId, role, request.storeId(), request.contentType(), request.fileSize())));
  }
}
