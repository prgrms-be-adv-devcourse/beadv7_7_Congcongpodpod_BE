package kr.lastdish.ai.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import kr.lastdish.ai.application.AiService;
import kr.lastdish.ai.exception.AiErrorCode;
import kr.lastdish.ai.presentation.dto.FoodClassificationResponse;
import kr.lastdish.common.api.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "AI Classification API", description = "음식 이미지 분류 도메인 API")
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

  private final AiService aiService;

  @Operation(summary = "S3 ObjectKey 기반 음식 카테고리 자동 분류")
  @PostMapping("/classify-s3")
  public ResponseEntity<FoodClassificationResponse> classifyByObjectKey(
      @RequestParam("objectKey") String objectKey) {

    FoodClassificationResponse response = aiService.classifyByObjectKey(objectKey);
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "음식 카테고리 자동 분류")
  @PostMapping(value = "/classify", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<FoodClassificationResponse> classify(
      @RequestPart("image") MultipartFile image,
      @RequestParam(value = "imageUrl", required = false) String imageUrl) {

    try {
      ByteArrayResource resource =
          new ByteArrayResource(image.getBytes()) {
            @Override
            public String getFilename() {
              return image.getOriginalFilename();
            }
          };

      FoodClassificationResponse response = aiService.classify(resource, imageUrl);
      return ResponseEntity.ok(response);

    } catch (IOException e) {
      // 이미지 파일 변환/읽기 실패 시 정확한 에러 코드로 전달
      throw new BusinessException(AiErrorCode.INVALID_IMAGE_FILE);
    }
  }
}
