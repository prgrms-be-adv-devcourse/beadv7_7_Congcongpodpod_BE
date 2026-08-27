package kr.lastdish.ai.elastic.presentation.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Builder
public class StoreSearchResult {
  private StoreResponse store;
  private double totalScore;
  private ScoreBreakdown scoreBreakdown;
  private List<String> badges;

  @Setter // RAG 결과를 나중에 채워 넣기 위해 이 필드만 mutable
  private String reason;

  @Getter
  @Builder
  public static class ScoreBreakdown {
    private double esScore;
    private double distanceScore;
    private double deadlineScore;
    private double priceScore;
    private double personalizationScore;
  }
}
