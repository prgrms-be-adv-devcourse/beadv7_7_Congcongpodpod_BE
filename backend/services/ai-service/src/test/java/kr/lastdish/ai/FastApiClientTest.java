package kr.lastdish.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

public class FastApiClientTest {

  @Test
  @DisplayName("FastAPI ngrok 서버로 test.jpg를 보내 실제 응답을 받아오는지 확인")
  void sendImageToFastApiServer() {
    // 1. 이미지 파일 준비
    File imageFile = new File("../../dev/local/ai-service/images/test.jpg");
    if (!imageFile.exists()) {
      System.err.println("test.jpg 파일이 프로젝트 루트 경로에 존재하지 않습니다.");
      return;
    }

    // 2. WebClient 설정 (ngrok 우회 헤더 및 버퍼 설정)
    ExchangeStrategies strategies =
        ExchangeStrategies.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
            .build();

    WebClient webClient =
        WebClient.builder()
            .baseUrl("https://mushily-hangup-smartness.ngrok-free.dev")
            .exchangeStrategies(strategies)
            .defaultHeader("ngrok-skip-browser-warning", "69420")
            .build();

    // 3. curl -F "image=@./test.jpg" 형태의 멀티파트 폼 데이터 생성
    MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
    bodyBuilder.part("image", new FileSystemResource(imageFile), MediaType.IMAGE_JPEG);

    System.out.println("FastAPI 서버로 이미지 분석 요청을 전송합니다...");

    // 4. POST 요청 전송 및 응답 출력
    String responseBody =
        webClient
            .post()
            .uri("/api/v1/classify-food")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
            .retrieve()
            .bodyToMono(String.class)
            .block();

    System.out.println("FastAPI 응답 결과:");
    System.out.println(responseBody);

    // 5. JUnit 검증
    assertThat(responseBody).isNotNull();
    assertThat(responseBody).contains("success");
  }
}
