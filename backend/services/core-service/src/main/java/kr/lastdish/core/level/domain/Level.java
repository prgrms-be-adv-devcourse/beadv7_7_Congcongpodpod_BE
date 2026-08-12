package kr.lastdish.core.level.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "levels", uniqueConstraints = @UniqueConstraint(columnNames = "member_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Level {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "level_id")
  private Long id;

  @Column(name = "member_id", nullable = false, updatable = false)
  private Long memberId;

  @Enumerated(EnumType.STRING)
  @Column(name = "dish_level", nullable = false)
  private DishLevel dishLevel;

  @Column(name = "purchase_count", nullable = false)
  private int purchaseCount;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  private Level(Long memberId) {
    this.memberId = memberId;
    this.dishLevel = DishLevel.LEVEL_1;
    this.purchaseCount = 0;
    this.updatedAt = LocalDateTime.now();
  }

  public static Level createDefault(Long memberId) {
    return new Level(memberId);
  }

  // 적립 완료 시 구매 횟수 증가
  public void addPurchase() {
    this.purchaseCount++;
    this.updatedAt = LocalDateTime.now();
  }

  // Level 재계산 및 승급 처리 (Level 승급 발생 시 true 반환)
  public boolean upgradeLevel() {
    DishLevel calculatedLevel = DishLevel.fromPurchaseCount(this.purchaseCount);
    if (this.dishLevel == calculatedLevel) {
      return false;
    }
    this.dishLevel = calculatedLevel;
    this.updatedAt = LocalDateTime.now();
    return true;
  }
}
