package kr.lastdish.ai.application;

import kr.lastdish.ai.application.dto.ClassificationResultDto;
import kr.lastdish.ai.domain.model.CategoryResult;
import kr.lastdish.ai.domain.model.ClassificationLog;
import kr.lastdish.ai.domain.repository.ClassificationLogRepository;
import kr.lastdish.ai.exception.AiErrorCode;
import kr.lastdish.ai.infrastructure.client.FastApiClient;
import kr.lastdish.ai.presentation.dto.FoodClassificationResponse;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.common.storage.application.PresignedUrlService;
import kr.lastdish.common.storage.application.dto.PresignedDownloadUrl;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiService {

  private final FastApiClient aiClient;
  private final ClassificationLogRepository logRepository;
  private final PresignedUrlService presignedUrlService;

  public FoodClassificationResponse classifyByObjectKey(String objectKey) {
    try {
      // 1. S3 Presigned Download URL 발급
      PresignedDownloadUrl downloadUrl = presignedUrlService.issueDownload(objectKey);

      // 2. Presigned GET URL을 Spring Resource(UrlResource)로 변환
      Resource imageResource = new UrlResource(downloadUrl.url());

      // 3. 기존 classify 메서드 호출
      return classify(imageResource, downloadUrl.url().toString());

    } catch (BusinessException e) {
      throw e;
    } catch (Exception e) {
      // UrlResource 생성 실패 or S3 조회 실패 예외 처리
      throw new BusinessException(AiErrorCode.INVALID_IMAGE_FILE);
    }
  }

  public FoodClassificationResponse classify(Resource imageResource, String imageUrl) {
    // 1. FastAPI 통신
    CategoryResult result = aiClient.classifyImage(imageResource);

    // 2. 1순위 카테고리 신뢰도가 15% 미만이면 예외 발생
    if (result.confidence() < 0.15) {
      throw new BusinessException(AiErrorCode.NOT_FOOD);
    }

    // 3. 분류 로그 생성 및 DB 저장
    ClassificationLog log =
        new ClassificationLog(
            imageUrl, result.predictedCategory(), result.confidence(), result.executionTimeMs());
    logRepository.save(log);

    // 4. 응답 DTO 변환 및 반환
    ClassificationResultDto resultDto = ClassificationResultDto.from(result);
    return FoodClassificationResponse.from(resultDto);
  }
}
