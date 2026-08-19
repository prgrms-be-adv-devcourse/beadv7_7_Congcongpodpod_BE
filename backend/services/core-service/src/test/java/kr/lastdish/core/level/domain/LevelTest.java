package kr.lastdish.core.level.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class LevelTest {

  @Test
  void createDefault로_생성하면_LEVEL_1_구매횟수_0으로_초기화된다() {
    Level level = Level.createDefault(1L);

    assertThat(level.getDishLevel()).isEqualTo(DishLevel.LEVEL_1);
    assertThat(level.getPurchaseCount()).isEqualTo(0);
  }

  @Test
  void addPurchase_호출하면_구매횟수가_1_증가한다() {
    Level level = Level.createDefault(1L);

    level.addPurchase();

    assertThat(level.getPurchaseCount()).isEqualTo(1);
  }

  @Test
  void addDiscountAmount_호출하면_할인금액이_누적된다() {
    Level level = Level.createDefault(1L);

    level.addDiscountAmount(new BigDecimal("3000"));
    level.addDiscountAmount(new BigDecimal("2000"));

    assertThat(level.getDiscountAmount()).isEqualByComparingTo(new BigDecimal("5000"));
  }

  @Test
  void 승급_기준을_충족하면_upgradeLevel은_true를_반환하고_등급이_바뀐다() {
    Level level = Level.createDefault(1L);
    for (int i = 0; i < 5; i++) {
      level.addPurchase(); // 구매횟수 5 -> LEVEL_2 기준 충족
    }

    boolean upgraded = level.upgradeLevel();

    assertThat(upgraded).isTrue();
    assertThat(level.getDishLevel()).isEqualTo(DishLevel.LEVEL_2);
  }

  @Test
  void 승급_기준을_충족하지_않으면_upgradeLevel은_false를_반환하고_등급이_유지된다() {
    Level level = Level.createDefault(1L);
    level.addPurchase(); // 구매횟수 1 -> 아직 LEVEL_1

    boolean upgraded = level.upgradeLevel();

    assertThat(upgraded).isFalse();
    assertThat(level.getDishLevel()).isEqualTo(DishLevel.LEVEL_1);
  }
}
