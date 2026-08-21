package kr.lastdish.ai.domain.model;

import java.util.List;

public record CategoryResult(
    String predictedCategory,
    double confidence,
    List<CategorySuggestion> suggestions,
    double executionTimeMs) {
  public record CategorySuggestion(String category, double confidence) {}
}
