package kr.lastdish.ai.domain.model;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ClassificationLog {
  private Long id;
  private String imageUrl;
  private String predictedCategory;
  private double confidence;
  private double executionTimeMs;
  private LocalDateTime createdAt;

  public ClassificationLog(
      String imageUrl, String predictedCategory, double confidence, double executionTimeMs) {
    this.imageUrl = imageUrl;
    this.predictedCategory = predictedCategory;
    this.confidence = confidence;
    this.executionTimeMs = executionTimeMs;
    this.createdAt = LocalDateTime.now();
  }
}
