package kr.lastdish.core.level.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "level_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LevelHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "level_history_id")
  private Long id;

  @Column(name = "member_id", nullable = false, updatable = false)
  private Long memberId;

  @Column(name = "order_id", nullable = false, updatable = false)
  private Long orderId;

  @Enumerated(EnumType.STRING)
  @Column(name = "old_level", nullable = false)
  private DishLevel oldLevel;

  @Enumerated(EnumType.STRING)
  @Column(name = "new_level", nullable = false)
  private DishLevel newLevel;

  @Column(name = "purchase_count_at_change", nullable = false)
  private int purchaseCountAtChange;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Builder(access = AccessLevel.PRIVATE)
  private LevelHistory(
      Long memberId,
      Long orderId,
      DishLevel oldLevel,
      DishLevel newLevel,
      int purchaseCountAtChange) {
    this.memberId = memberId;
    this.orderId = orderId;
    this.oldLevel = oldLevel;
    this.newLevel = newLevel;
    this.purchaseCountAtChange = purchaseCountAtChange;
    this.createdAt = LocalDateTime.now();
  }

  public static LevelHistory recordPurchase(
      Long memberId,
      Long orderId,
      DishLevel oldLevel,
      DishLevel newLevel,
      int purchaseCountAtChange) {
    return LevelHistory.builder()
        .memberId(memberId)
        .orderId(orderId)
        .oldLevel(oldLevel)
        .newLevel(newLevel)
        .purchaseCountAtChange(purchaseCountAtChange)
        .build();
  }

  public boolean isUpgrade() {
    return oldLevel != newLevel;
  }
}
