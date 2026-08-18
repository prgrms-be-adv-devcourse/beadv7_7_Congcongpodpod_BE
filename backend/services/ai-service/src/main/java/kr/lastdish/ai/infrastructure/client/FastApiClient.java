package kr.lastdish.ai.infrastructure.client;

import kr.lastdish.ai.domain.model.CategoryResult;
import kr.lastdish.ai.infrastructure.client.dto.FastApiResponse;
import lombok.RequiredArgsConstructor;
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
        .map(FastApiResponse::toDomain);
  }
}
