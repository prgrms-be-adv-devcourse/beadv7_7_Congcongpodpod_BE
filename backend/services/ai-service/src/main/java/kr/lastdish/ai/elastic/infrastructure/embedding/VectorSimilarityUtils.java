package kr.lastdish.ai.elastic.infrastructure.embedding;

import java.util.List;

/** 필드별 벡터 유사도(코사인 유사도)를 계산하는 유틸리티. */
public final class VectorSimilarityUtils {

  private VectorSimilarityUtils() {}

  /**
   * 두 벡터의 코사인 유사도를 계산한다 ([-1, 1] 범위). 둘 중 하나라도 null/empty이거나 차원이 다르면 0.0을 반환한다 (임베딩 실패/누락 필드 방어).
   */
  public static double cosineSimilarity(List<Float> a, List<Float> b) {
    if (a == null || b == null || a.isEmpty() || b.isEmpty() || a.size() != b.size()) {
      return 0.0;
    }

    double dot = 0.0;
    double normA = 0.0;
    double normB = 0.0;
    for (int i = 0; i < a.size(); i++) {
      double x = a.get(i);
      double y = b.get(i);
      dot += x * y;
      normA += x * x;
      normB += y * y;
    }

    if (normA == 0.0 || normB == 0.0) {
      return 0.0;
    }

    return dot / (Math.sqrt(normA) * Math.sqrt(normB));
  }
}
