package kr.lastdish.ai.infrastructure.client;

import kr.lastdish.ai.domain.model.CategoryResult;
import kr.lastdish.ai.exception.AiErrorCode;
import kr.lastdish.ai.infrastructure.client.dto.FastApiResponse;
import kr.lastdish.common.api.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class FastApiClient {

  private final RestClient restClient;

  @Value("${ai.engine.url}")
  private String aiEngineUrl;

  public CategoryResult classifyImage(Resource imageResource) {
    MultipartBodyBuilder builder = new MultipartBodyBuilder();
    builder.part("image", imageResource);

    try {
      FastApiResponse response =
          restClient
              .post()
              .uri(aiEngineUrl + "/api/v1/classify-food")
              .contentType(MediaType.MULTIPART_FORM_DATA)
              .body(builder.build())
              .retrieve()
              .body(FastApiResponse.class);

      if (response == null) {
        throw new BusinessException(AiErrorCode.AI_SERVER_ERROR);
      }

      return response.toDomain();

    } catch (BusinessException e) {
      // 이미 정의된 비즈니스 예외는 그대로 재전파
      throw e;
    } catch (ResourceAccessException e) {
      // 읽기/연결 타임아웃 발생 시
      log.warn("FastAPI 응답 시간 초과 또는 연결 실패: {}", e.getMessage());
      throw new BusinessException(AiErrorCode.TIMEOUT_ERROR);
    } catch (Exception e) {
      log.error("FastAPI 통신 실패 - Error: {}", e.getMessage());
      throw new BusinessException(AiErrorCode.AI_SERVER_ERROR);
    }
  }
}
