package kr.lastdish.core.dish.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class DishTest {

  @Test
  void created_dish_keeps_category() {
    Dish dish = createDish(10L);

    assertThat(dish.getCategory()).isEqualTo("한식");
  }

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

    dish.decreaseStock(1L);

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

  @Test
  void Dish를_수정해도_이미지_key는_변경되지_않는다() {
    Dish dish = createDish(10L);

    dish.update(
        "된장찌개",
        LocalDateTime.now(),
        "수정된 상품 설명",
        BigDecimal.valueOf(12_000),
        BigDecimal.valueOf(8_000),
        LocalTime.of(17, 0),
        LocalTime.of(20, 0));

    assertThat(dish.getThumbnailUrl()).isEqualTo("dish/1/test.jpg");
  }

  /** 테스트에 필요한 기본 판매 중 Dish를 생성합니다. */
  private Dish createDish(Long stockQuantity) {
    return Dish.create(
        1L,
        "김치찌개",
        LocalDateTime.now(),
        "상품 설명",
        "한식",
        "dish/1/test.jpg",
        stockQuantity,
        BigDecimal.valueOf(10000),
        BigDecimal.ZERO,
        LocalTime.of(18, 0),
        LocalTime.of(19, 0));
  }
}
