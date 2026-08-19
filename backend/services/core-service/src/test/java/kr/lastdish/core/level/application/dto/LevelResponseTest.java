package kr.lastdish.core.level.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import kr.lastdish.core.level.domain.DishLevel;
import kr.lastdish.core.level.domain.Level;
import org.junit.jupiter.api.Test;

class LevelResponseTest {

  @Test
  void from_최고_등급이_아니면_다음_등급까지_남은_횟수를_계산한다() {
    Level level = Level.createDefault(1L);
    for (int i = 0; i < 2; i++) {
      level.addPurchase(); // 구매횟수 2, LEVEL_1 유지 (LEVEL_2 기준 5회)
    }

    LevelResponse response = LevelResponse.from(level);

    assertThat(response.dishLevel()).isEqualTo(DishLevel.LEVEL_1);
    assertThat(response.purchaseCount()).isEqualTo(2);
    assertThat(response.remainToNextLevel()).isEqualTo(3); // 5 - 2
  }

  @Test
  void from_최고_등급이면_남은_횟수는_0이다() {
    Level level = Level.createDefault(1L);
    for (int i = 0; i < 20; i++) {
      level.addPurchase();
    }
    level.upgradeLevel(); // LEVEL_5로 승급

    LevelResponse response = LevelResponse.from(level);

    assertThat(response.dishLevel()).isEqualTo(DishLevel.LEVEL_5);
    assertThat(response.remainToNextLevel()).isEqualTo(0);
  }

  @Test
  void from_할인금액도_그대로_담긴다() {
    Level level = Level.createDefault(1L);
    level.addDiscountAmount(new BigDecimal("7500"));

    LevelResponse response = LevelResponse.from(level);

    assertThat(response.discountAmount()).isEqualByComparingTo(new BigDecimal("7500"));
  }
}
