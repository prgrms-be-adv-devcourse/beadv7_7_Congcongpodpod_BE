package kr.lastdish.ai.application;

import kr.lastdish.ai.application.dto.ClassificationResultDto;
import kr.lastdish.ai.domain.model.ClassificationLog;
import kr.lastdish.ai.domain.repository.ClassificationLogRepository;
import kr.lastdish.ai.exception.AiErrorCode;
import kr.lastdish.ai.infrastructure.client.FastApiClient;
import kr.lastdish.ai.presentation.dto.FoodClassificationResponse;
import kr.lastdish.common.api.exception.BusinessException; // 공통 BusinessException import
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AiService {

  private final FastApiClient aiClient;
  private final ClassificationLogRepository logRepository;

  public Mono<FoodClassificationResponse> classify(Resource imageResource, String imageUrl) {
    return aiClient
        .classifyImage(imageResource)
        .flatMap(
            result -> {
              // 1순위 카테고리 신뢰도가 15% 미만이면 예외 발생
              if (result.confidence() < 0.15) {
                return Mono.error(new BusinessException(AiErrorCode.NOT_FOOD));
              }

              ClassificationLog log =
                  new ClassificationLog(
                      imageUrl,
                      result.predictedCategory(),
                      result.confidence(),
                      result.executionTimeMs());
              return logRepository.save(log).thenReturn(result);
            })
        .map(ClassificationResultDto::from)
        .map(FoodClassificationResponse::from);
  }
}
