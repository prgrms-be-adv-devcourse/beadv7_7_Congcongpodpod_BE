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

  @Test
  void Dish가_판매불가이면_장바구니_상품도_판매불가로_변경한다() {
    // given
    CartItem cartItem = CartItem.create(1L, 10L, "김치찌개", BigDecimal.valueOf(8_000), 2L);

    // when
    cartItem.synchronizeDishState(false, 10L);

    // then
    assertThat(cartItem.getStatus()).isEqualTo(CartItemStatus.DISH_UNAVAILABLE);
    assertThat(cartItem.isOrderable()).isFalse();
  }

  // 아래 부터는 이벤트 테스트를 위한 코드입니다 ---------------------------------------------------------

  @Test
  void Dish_재고가_없으면_품절로_변경한다() {
    // given
    CartItem cartItem = CartItem.create(1L, 10L, "김치찌개", BigDecimal.valueOf(8_000), 2L);

    // when
    cartItem.synchronizeDishState(true, 0L);

    // then
    assertThat(cartItem.getStatus()).isEqualTo(CartItemStatus.OUT_OF_STOCK);
    assertThat(cartItem.isOrderable()).isFalse();
  }

  @Test
  void 장바구니_수량보다_Dish_재고가_적으면_재고부족으로_변경한다() {
    // given
    CartItem cartItem = CartItem.create(1L, 10L, "김치찌개", BigDecimal.valueOf(8_000), 7L);

    // when
    cartItem.synchronizeDishState(true, 5L);

    // then
    assertThat(cartItem.getStatus()).isEqualTo(CartItemStatus.INSUFFICIENT_STOCK);
    assertThat(cartItem.isOrderable()).isFalse();
  }

  @Test
  void Dish_재고가_장바구니_수량_이상이면_주문가능으로_변경한다() {
    // given
    CartItem cartItem = CartItem.create(1L, 10L, "김치찌개", BigDecimal.valueOf(8_000), 2L);

    cartItem.synchronizeDishState(true, 0L);

    // when
    cartItem.synchronizeDishState(true, 5L);

    // then
    assertThat(cartItem.getStatus()).isEqualTo(CartItemStatus.AVAILABLE);
    assertThat(cartItem.isOrderable()).isTrue();
  }

  @Test
  void 주문불가였던_상품을_검증된_Dish로_교체하면_주문가능으로_변경한다() {
    // given
    CartItem cartItem = CartItem.create(1L, 10L, "기존 상품", BigDecimal.valueOf(3_000), 5L);

    cartItem.synchronizeDishState(true, 3L);

    assertThat(cartItem.getStatus()).isEqualTo(CartItemStatus.INSUFFICIENT_STOCK);

    // when
    cartItem.replace(20L, "교체 상품", BigDecimal.valueOf(5_000), 2L);

    // then
    assertThat(cartItem.getStatus()).isEqualTo(CartItemStatus.AVAILABLE);

    assertThat(cartItem.isOrderable()).isTrue();
  }

  @Test
  void 재고부족이었던_상품의_수량을_주문가능하게_변경하면_상태도_복구한다() {
    // given
    CartItem cartItem = CartItem.create(1L, 10L, "김치찌개", BigDecimal.valueOf(8_000), 5L);

    cartItem.synchronizeDishState(true, 3L);

    assertThat(cartItem.getStatus()).isEqualTo(CartItemStatus.INSUFFICIENT_STOCK);

    // when
    cartItem.changeQuantity(2L);

    // then
    assertThat(cartItem.getQuantity()).isEqualTo(2L);

    assertThat(cartItem.getStatus()).isEqualTo(CartItemStatus.AVAILABLE);

    assertThat(cartItem.isOrderable()).isTrue();
  }
}
