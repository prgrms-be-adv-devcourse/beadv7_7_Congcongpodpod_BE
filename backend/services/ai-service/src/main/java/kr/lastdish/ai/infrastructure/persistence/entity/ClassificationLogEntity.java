package kr.lastdish.ai.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ai_classification_log")
@Getter
@NoArgsConstructor
public class ClassificationLogEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String imageUrl;
  private String predictedCategory;
  private double confidence;
  private double executionTimeMs;
  private LocalDateTime createdAt;

  // 4개 인자를 받는 생성자 추가
  public ClassificationLogEntity(
      String imageUrl, String predictedCategory, double confidence, double executionTimeMs) {
    this.imageUrl = imageUrl;
    this.predictedCategory = predictedCategory;
    this.confidence = confidence;
    this.executionTimeMs = executionTimeMs;
    this.createdAt = LocalDateTime.now();
  }
}
