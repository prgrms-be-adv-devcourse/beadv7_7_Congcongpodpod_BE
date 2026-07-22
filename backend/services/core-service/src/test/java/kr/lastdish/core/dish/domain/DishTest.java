package kr.lastdish.core.dish.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class DishTest {

  @Test
  void on_sale_dish_with_stock_is_available() {
    // given
    Dish dish = createDish(10L);

    // when
    boolean available = dish.isAvailable();

    // then
    assertThat(available).isTrue();
  }

  @Test
  void dish_without_stock_is_unavailable() {
    // given
    Dish dish = createDish(1L);

    /*
     * 실제 상품 수정과 동일한 도메인 메서드를 사용하여
     * 재고를 0으로 변경합니다.
     */
    dish.update(
        "김치찌개",
        LocalDateTime.now(),
        "상품 설명",
        Category.KOREAN,
        null,
        0L,
        BigDecimal.valueOf(10000),
        BigDecimal.ZERO);

    // when
    boolean available = dish.isAvailable();

    // then
    assertThat(available).isFalse();
  }

  @Test
  void deleted_dish_is_unavailable() {
    // given
    Dish dish = createDish(10L);

    // when
    dish.delete();

    // then
    assertThat(dish.isAvailable()).isFalse();
  }

  /** 테스트에 필요한 기본 판매 중 Dish를 생성합니다. */
  private Dish createDish(Long stockQuantity) {
    return Dish.create(
        1L,
        "김치찌개",
        LocalDateTime.now(),
        "상품 설명",
        Category.KOREAN,
        null,
        stockQuantity,
        BigDecimal.valueOf(10000),
        BigDecimal.ZERO);
  }
}
