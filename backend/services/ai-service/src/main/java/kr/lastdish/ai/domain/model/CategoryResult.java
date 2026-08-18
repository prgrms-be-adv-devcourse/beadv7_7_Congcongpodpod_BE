package kr.lastdish.ai.domain.model;

import java.util.Collections;
import java.util.List;

public record CategoryResult(
    String predictedCategory,
    double confidence,
    List<CategorySuggestion> suggestions,
    double executionTimeMs) {
  public record CategorySuggestion(String category, double confidence) {}

  // 타임아웃/에러 시 사용할 Fallback 객체
  public static CategoryResult fallback(String reason) {
    return new CategoryResult("기타", 0.0, Collections.emptyList(), 0.0);
  }
}
