package kr.lastdish.ai.application;

import kr.lastdish.ai.application.dto.ClassificationResultDto;
import kr.lastdish.ai.domain.model.CategoryResult;
import kr.lastdish.ai.exception.AiErrorCode;
import kr.lastdish.ai.infrastructure.client.FastApiClient;
import kr.lastdish.ai.presentation.dto.FoodClassificationResponse;
import kr.lastdish.common.api.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiService {

  private final FastApiClient aiClient;

  public FoodClassificationResponse classify(Resource imageResource) {
    // 1. FastAPI 통신
    CategoryResult result = aiClient.classifyImage(imageResource);

    // 2. 1순위 카테고리 신뢰도가 15% 미만이면 예외 발생
    if (result.confidence() < 0.15) {
      throw new BusinessException(AiErrorCode.NOT_FOOD);
    }

    // 3. 응답 DTO 변환 및 반환
    ClassificationResultDto resultDto = ClassificationResultDto.from(result);
    return FoodClassificationResponse.from(resultDto);
  }
}
