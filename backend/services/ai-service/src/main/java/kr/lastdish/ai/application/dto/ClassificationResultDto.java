package kr.lastdish.ai.application.dto;

import java.util.List;
import kr.lastdish.ai.domain.model.CategoryResult;

public record ClassificationResultDto(
    boolean success,
    String predictedCategory,
    double confidence,
    List<SuggestionDto> suggestions,
    double executionTimeMs) {
  public record SuggestionDto(String category, double confidence) {}

  public static ClassificationResultDto from(CategoryResult domain) {
    List<SuggestionDto> suggestions =
        domain.suggestions() == null
            ? List.of()
            : domain.suggestions().stream()
                .map(s -> new SuggestionDto(s.category(), s.confidence()))
                .toList();

    return new ClassificationResultDto(
        true,
        domain.predictedCategory(),
        domain.confidence(),
        suggestions,
        domain.executionTimeMs());
  }
}
