package kr.lastdish.ai.infrastructure.client;

import java.time.Duration;
import java.util.concurrent.TimeoutException;
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
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * FastAPI AI 엔진 통신 클라이언트
 *
 * <p>Spring WebClient를 사용해 외부 FastAPI 서버로 이미지 분석 요청을 전송하고 응답을 받음
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FastApiClient {

  private final WebClient webClient;

  @Value("${ai.engine.url}")
  private String aiEngineUrl;

  public Mono<CategoryResult> classifyImage(Resource imageResource) {
    MultipartBodyBuilder builder = new MultipartBodyBuilder();
    builder.part("image", imageResource);

    return webClient
        .post()
        .uri(aiEngineUrl + "/api/v1/classify-food")
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(builder.build()))
        .retrieve()
        .bodyToMono(FastApiResponse.class)
        .map(FastApiResponse::toDomain)
        // 1. 응답 대기 시간 제한 (3초)
        .timeout(Duration.ofSeconds(3))
        // 2. 타임아웃 발생 시
        .onErrorResume(
            TimeoutException.class,
            e -> {
              log.warn("FastAPI 응답 시간 초과 (3초)");
              return Mono.error(new BusinessException(AiErrorCode.TIMEOUT_ERROR));
            })
        // 3. 기타 네트워크/통신 에러 발생 시
        .onErrorResume(
            Exception.class,
            e -> {
              log.error("FastAPI 통신 실패 - Error: {}", e.getMessage());
              return Mono.error(new BusinessException(AiErrorCode.AI_SERVER_ERROR));
            });
  }
}
