package kr.lastdish.ai.foodclassify.infrastructure.dto;

import java.util.List;
import kr.lastdish.ai.foodclassify.domain.model.CategoryResult;
import kr.lastdish.ai.foodclassify.domain.model.CategoryResult.CategorySuggestion;

public record FastApiResponse(
    boolean success,
    String predictedCategory,
    double confidence,
    List<SuggestionDto> suggestions,
    double executionTimeMs) {
  public record SuggestionDto(String category, double confidence) {}

  // FastAPI DTO를 순수 도메인 모델인 CategoryResult로 변환
  public CategoryResult toDomain() {
    List<CategorySuggestion> domainSuggestions =
        suggestions == null
            ? List.of()
            : suggestions.stream()
                .map(s -> new CategorySuggestion(s.category(), s.confidence()))
                .toList();

    return new CategoryResult(predictedCategory, confidence, domainSuggestions, executionTimeMs);
  }
}
