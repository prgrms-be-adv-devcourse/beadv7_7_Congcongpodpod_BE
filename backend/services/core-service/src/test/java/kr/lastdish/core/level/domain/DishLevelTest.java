package kr.lastdish.core.level.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DishLevelTest {

  @ParameterizedTest
  @CsvSource({
    "0, LEVEL_1",
    "4, LEVEL_1",
    "5, LEVEL_2",
    "9, LEVEL_2",
    "10, LEVEL_3",
    "14, LEVEL_3",
    "15, LEVEL_4",
    "19, LEVEL_4",
    "20, LEVEL_5",
    "100, LEVEL_5"
  })
  void 구매횟수에_따라_올바른_등급을_반환한다(int purchaseCount, DishLevel expected) {
    DishLevel result = DishLevel.fromPurchaseCount(purchaseCount);

    assertThat(result).isEqualTo(expected);
  }

  @Test
  void next_최고_등급이_아니면_다음_등급을_반환한다() {
    assertThat(DishLevel.LEVEL_1.next()).isEqualTo(DishLevel.LEVEL_2);
    assertThat(DishLevel.LEVEL_4.next()).isEqualTo(DishLevel.LEVEL_5);
  }

  @Test
  void next_최고_등급이면_null을_반환한다() {
    assertThat(DishLevel.LEVEL_5.next()).isNull();
  }
}
