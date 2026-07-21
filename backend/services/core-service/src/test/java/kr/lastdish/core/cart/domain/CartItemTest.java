package kr.lastdish.core.cart.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CartItemTest {

  @Test
  void 상품_추가시_소계는_단가와_수량의_곱이다() {
    CartItem cartItem = CartItem.create(1L, 10L, "치킨마요 마감할인 세트", BigDecimal.valueOf(3000), 2L);

    assertThat(cartItem.getSubtotalPrice()).isEqualByComparingTo(BigDecimal.valueOf(6000));
  }

  @Test
  void 수량_변경시_소계가_다시_계산된다() {
    CartItem cartItem = CartItem.create(1L, 10L, "치킨마요 마감할인 세트", BigDecimal.valueOf(3000), 1L);

    cartItem.changeQuantity(3L);

    assertThat(cartItem.getQuantity()).isEqualTo(3L);
    assertThat(cartItem.getSubtotalPrice()).isEqualByComparingTo(BigDecimal.valueOf(9000));
  }

  @Test
  void 수량을_0_이하로_바꾸면_예외가_발생한다() {
    CartItem cartItem = CartItem.create(1L, 10L, "치킨마요 마감할인 세트", BigDecimal.valueOf(3000), 1L);

    assertThatThrownBy(() -> cartItem.changeQuantity(0L))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void 상품_교체시_dishId와_이름과_단가와_수량이_모두_바뀐다() {
    CartItem cartItem = CartItem.create(1L, 10L, "치킨마요 마감할인 세트", BigDecimal.valueOf(3000), 1L);

    cartItem.replace(20L, "소불고기 마감할인 세트", BigDecimal.valueOf(5000), 2L);

    assertThat(cartItem.getDishId()).isEqualTo(20L);
    assertThat(cartItem.getDishName()).isEqualTo("소불고기 마감할인 세트");
    assertThat(cartItem.getUnitPrice()).isEqualByComparingTo(BigDecimal.valueOf(5000));
    assertThat(cartItem.getQuantity()).isEqualTo(2L);
  }
}
