package kr.lastdish.ai.elastic.infrastructure.embedding;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingService {

  private final EmbeddingModel embeddingModel;

  public List<Float> getEmbeddingList(String text) {
    if (text == null || text.isBlank()) {
      return null;
    }
    List<List<Float>> results = getEmbeddingBatch(List.of(text));
    return results.isEmpty() ? null : results.get(0);
  }

  /**
   * 여러 텍스트를 한 번의 API 호출로 임베딩, 색인 시 필드별로 바뀐 텍스트만 모아서 이 메서드를 한 번 호출 배치 호출 자체가 실패하면 모든 위치에 null을 채워
   * 반환하고, 색인 자체가 막히지 않도록, 예외를 상위로 던지지 않음
   */
  public List<List<Float>> getEmbeddingBatch(List<String> texts) {
    if (texts == null || texts.isEmpty()) {
      return List.of();
    }

    List<String> safeTexts = texts.stream().map(t -> t == null ? "" : t).toList();

    try {
      EmbeddingResponse response = embeddingModel.embedForResponse(safeTexts);
      List<Embedding> results = response.getResults();

      List<List<Float>> out = new ArrayList<>(safeTexts.size());
      for (int i = 0; i < safeTexts.size(); i++) {
        if (i >= results.size()) {
          out.add(null);
          continue;
        }
        float[] vector = results.get(i).getOutput();
        List<Float> floatList = new ArrayList<>(vector.length);
        for (float v : vector) {
          floatList.add(v);
        }
        out.add(floatList);
      }
      return out;

    } catch (Exception e) {
      log.error("배치 텍스트 임베딩 생성 실패 - 대상 텍스트 수: {}", safeTexts.size(), e);
      List<List<Float>> failed = new ArrayList<>(safeTexts.size());
      for (int i = 0; i < safeTexts.size(); i++) {
        failed.add(null);
      }
      return failed;
    }
  }
}
