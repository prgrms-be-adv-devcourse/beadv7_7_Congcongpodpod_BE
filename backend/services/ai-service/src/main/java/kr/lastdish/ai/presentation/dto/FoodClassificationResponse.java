package kr.lastdish.ai.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.lastdish.ai.application.dto.ClassificationResultDto;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "음식 이미지 분류 결과 응답 DTO")
public class FoodClassificationResponse {

  @Schema(description = "예측된 음식 카테고리")
  private final String predictedCategory;

  @Schema(description = "예측 신뢰도 (0.0 ~ 1.0)")
  private final double confidence;

  @Schema(description = "FastAPI 모델 실행 시간(ms)")
  private final double executionTimeMs;

  public static FoodClassificationResponse from(ClassificationResultDto dto) {
    return FoodClassificationResponse.builder()
        .predictedCategory(dto.predictedCategory())
        .confidence(dto.confidence())
        .executionTimeMs(dto.executionTimeMs())
        .build();
  }
}
