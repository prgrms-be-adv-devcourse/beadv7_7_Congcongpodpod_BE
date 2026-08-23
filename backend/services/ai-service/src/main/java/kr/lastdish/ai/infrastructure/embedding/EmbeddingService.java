package kr.lastdish.ai.infrastructure.embedding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingService {

  private final EmbeddingModel embeddingModel;

  public List<Float> getEmbeddingList(String text) {
    if (text == null || text.isBlank()) {
      return Collections.emptyList();
    }

    try {
      float[] vector =
          embeddingModel
              .embedForResponse(List.of(text))
              .getResults()
              .getFirst() // 단일 텍스트 요청에 대한 첫 번째 임베딩 결과 추출
              .getOutput();

      List<Float> result = new ArrayList<>(vector.length);
      for (float v : vector) {
        result.add(v);
      }
      return result;

    } catch (Exception e) {
      log.error("텍스트 임베딩 생성 실패 - 대상 텍스트: {}", text, e);
      return Collections.emptyList();
    }
  }
}
