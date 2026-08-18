package kr.lastdish.ai.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.lastdish.ai.application.AiService;
import kr.lastdish.ai.presentation.dto.FoodClassificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Tag(name = "AI Classification API", description = "음식 이미지 분류 도메인 API")
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

  private final AiService aiService;

  @Operation(summary = "음식 카테고리 자동 분류")
  @PostMapping(value = "/classify", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public Mono<ResponseEntity<FoodClassificationResponse>> classify(
      @RequestPart("image") FilePart image,
      @RequestParam(value = "imageUrl", required = false) String imageUrl) {

    return DataBufferUtils.join(image.content())
        .flatMap(
            dataBuffer -> {
              byte[] bytes = new byte[dataBuffer.readableByteCount()];
              dataBuffer.read(bytes);
              DataBufferUtils.release(dataBuffer);

              // FastAPI가 파일명을 다룰 수 있도록 getFilename() 지정
              ByteArrayResource resource =
                  new ByteArrayResource(bytes) {
                    @Override
                    public String getFilename() {
                      return image.filename();
                    }
                  };

              return aiService.classify(resource, imageUrl);
            })
        .map(ResponseEntity::ok);
  }
}
