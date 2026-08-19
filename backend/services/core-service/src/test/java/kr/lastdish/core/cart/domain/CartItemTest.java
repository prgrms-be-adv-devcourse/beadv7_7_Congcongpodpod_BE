package kr.lastdish.core.cart.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CartItemTest {

  // 이 테스트가 확인하려는 건 cartId/dishId/픽업시간이 아니라 가격·수량·상태라, 나머지는 헬퍼로 고정한다.
  // 정가를 따지지 않는 테스트는 할인 없음(정가 = 판매가)으로 둔다.
  private static CartItem cartItem(String dishName, BigDecimal unitPrice, Long quantity) {
    return cartItem(dishName, unitPrice, quantity, 0L);
  }

  private static CartItem cartItem(
      String dishName, BigDecimal unitPrice, Long quantity, long dishVersion) {
    return cartItem(dishName, unitPrice, unitPrice, quantity, dishVersion);
  }

  private static CartItem cartItem(
      String dishName,
      BigDecimal dishPrice,
      BigDecimal unitPrice,
      Long quantity,
      long dishVersion) {
    return CartItem.create(
        1L, 10L, null, dishName, dishPrice, unitPrice, quantity, null, null, dishVersion);
  }

  @Test
  void 상품_추가시_소계는_단가와_수량의_곱이다() {
    CartItem cartItem = cartItem("치킨마요 마감할인 세트", BigDecimal.valueOf(3000), 2L);

    assertThat(cartItem.getSubtotalPrice()).isEqualByComparingTo(BigDecimal.valueOf(6000));
  }

  @Test
  void 수량_변경시_소계가_다시_계산된다() {
    CartItem cartItem = cartItem("치킨마요 마감할인 세트", BigDecimal.valueOf(3000), 1L);

    cartItem.changeQuantity(3L);

    assertThat(cartItem.getQuantity()).isEqualTo(3L);
    assertThat(cartItem.getSubtotalPrice()).isEqualByComparingTo(BigDecimal.valueOf(9000));
  }

  @Test
  void 수량을_0_이하로_바꾸면_예외가_발생한다() {
    CartItem cartItem = cartItem("치킨마요 마감할인 세트", BigDecimal.valueOf(3000), 1L);

    assertThatThrownBy(() -> cartItem.changeQuantity(0L))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void 수량이_0_이하인_상품은_담을_수_없다() {
    assertThatThrownBy(() -> cartItem("치킨마요 마감할인 세트", BigDecimal.valueOf(3000), 0L))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void 정가가_판매가보다_낮은_상품은_담을_수_없다() {
    assertThatThrownBy(
            () ->
                cartItem(
                    "치킨마요 마감할인 세트", BigDecimal.valueOf(3000), BigDecimal.valueOf(5000), 1L, 0L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Dish 정가는 판매 가격 이상이어야 합니다.");
  }

  @Test
  void 정가가_판매가보다_낮은_상품으로는_교체할_수_없다() {
    CartItem cartItem = cartItem("치킨마요 마감할인 세트", BigDecimal.valueOf(3000), 1L);

    assertThatThrownBy(
            () ->
                cartItem.replace(
                    20L,
                    null,
                    "소불고기 마감할인 세트",
                    BigDecimal.valueOf(3000),
                    BigDecimal.valueOf(5000),
                    2L,
                    null,
                    null,
                    0L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Dish 정가는 판매 가격 이상이어야 합니다.");
  }

  @Test
  void 판매가가_음수인_상품은_담을_수_없다() {
    assertThatThrownBy(
            () ->
                cartItem("치킨마요 마감할인 세트", BigDecimal.valueOf(3000), BigDecimal.valueOf(-1), 1L, 0L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Dish 판매 가격은 0 이상이어야 합니다.");
  }

  @Test
  void 상품_교체시_dishId와_이름과_단가와_수량이_모두_바뀐다() {
    CartItem cartItem = cartItem("치킨마요 마감할인 세트", BigDecimal.valueOf(3000), 1L);

    cartItem.replace(
        20L,
        null,
        "소불고기 마감할인 세트",
        BigDecimal.valueOf(8000),
        BigDecimal.valueOf(5000),
        2L,
        null,
        null,
        0L);

    assertThat(cartItem.getDishId()).isEqualTo(20L);
    assertThat(cartItem.getDishName()).isEqualTo("소불고기 마감할인 세트");
    assertThat(cartItem.getDishPrice()).isEqualByComparingTo(BigDecimal.valueOf(8000));
    assertThat(cartItem.getUnitPrice()).isEqualByComparingTo(BigDecimal.valueOf(5000));
    assertThat(cartItem.getQuantity()).isEqualTo(2L);
  }

  @Test
  void Dish가_판매불가이면_장바구니_상품도_판매불가로_변경한다() {
    // given
    CartItem cartItem = cartItem("김치찌개", BigDecimal.valueOf(8_000), 2L);

    // when
    cartItem.synchronizeDishState(false, 10L, 1L);

    // then
    assertThat(cartItem.getStatus()).isEqualTo(CartItemStatus.DISH_UNAVAILABLE);
    assertThat(cartItem.isOrderable()).isFalse();
  }

  // 아래 부터는 이벤트 테스트를 위한 코드입니다 ---------------------------------------------------------

  @Test
  void Dish_재고가_없으면_품절로_변경한다() {
    // given
    CartItem cartItem = cartItem("김치찌개", BigDecimal.valueOf(8_000), 2L);

    // when
    cartItem.synchronizeDishState(true, 0L, 1L);

    // then
    assertThat(cartItem.getStatus()).isEqualTo(CartItemStatus.OUT_OF_STOCK);
    assertThat(cartItem.isOrderable()).isFalse();
  }

  @Test
  void 장바구니_수량보다_Dish_재고가_적으면_재고부족으로_변경한다() {
    // given
    CartItem cartItem = cartItem("김치찌개", BigDecimal.valueOf(8_000), 7L);

    // when
    cartItem.synchronizeDishState(true, 5L, 1L);

    // then
    assertThat(cartItem.getStatus()).isEqualTo(CartItemStatus.INSUFFICIENT_STOCK);
    assertThat(cartItem.isOrderable()).isFalse();
  }

  @Test
  void Dish_재고가_장바구니_수량_이상이면_주문가능으로_변경한다() {
    // given
    CartItem cartItem = cartItem("김치찌개", BigDecimal.valueOf(8_000), 2L);

    cartItem.synchronizeDishState(true, 0L, 1L);

    // when
    cartItem.synchronizeDishState(true, 5L, 2L);

    // then
    assertThat(cartItem.getStatus()).isEqualTo(CartItemStatus.AVAILABLE);
    assertThat(cartItem.isOrderable()).isTrue();
  }

  @Test
  void 주문불가였던_상품을_검증된_Dish로_교체하면_주문가능으로_변경한다() {
    // given
    CartItem cartItem = cartItem("기존 상품", BigDecimal.valueOf(3_000), 5L);

    cartItem.synchronizeDishState(true, 3L, 1L);

    assertThat(cartItem.getStatus()).isEqualTo(CartItemStatus.INSUFFICIENT_STOCK);

    // when
    cartItem.replace(
        20L,
        null,
        "교체 상품",
        BigDecimal.valueOf(5_000),
        BigDecimal.valueOf(5_000),
        2L,
        null,
        null,
        0L);

    // then
    assertThat(cartItem.getStatus()).isEqualTo(CartItemStatus.AVAILABLE);

    assertThat(cartItem.isOrderable()).isTrue();
  }

  @Test
  void 재고부족이었던_상품의_수량을_주문가능하게_변경하면_상태도_복구한다() {
    // given
    CartItem cartItem = cartItem("김치찌개", BigDecimal.valueOf(8_000), 5L);

    cartItem.synchronizeDishState(true, 3L, 1L);

    assertThat(cartItem.getStatus()).isEqualTo(CartItemStatus.INSUFFICIENT_STOCK);

    // when
    cartItem.changeQuantity(2L);

    // then
    assertThat(cartItem.getQuantity()).isEqualTo(2L);

    assertThat(cartItem.getStatus()).isEqualTo(CartItemStatus.AVAILABLE);

    assertThat(cartItem.isOrderable()).isTrue();
  }

  @Test
  void 같은_버전과_이전_버전의_Dish_이벤트는_무시한다() {
    // given
    CartItem cartItem = cartItem("김치찌개", BigDecimal.valueOf(8_000), 2L, 2L);

    // when
    cartItem.synchronizeDishState(false, 0L, 2L);
    cartItem.synchronizeDishState(false, 0L, 1L);

    // then
    assertThat(cartItem.getStatus()).isEqualTo(CartItemStatus.AVAILABLE);
    assertThat(cartItem.getLastAppliedDishVersion()).isEqualTo(2L);
  }

  @Test
  void 가격이_변경되어도_장바구니에_담을_때의_가격을_보존한다() {
    // given
    CartItem cartItem =
        cartItem("김치찌개", BigDecimal.valueOf(10_000), BigDecimal.valueOf(8_000), 2L, 1L);

    // when
    cartItem.synchronizeDishPrice(BigDecimal.valueOf(10_000), BigDecimal.valueOf(7_000), 2L);

    // then
    assertThat(cartItem.getDishPrice()).isEqualByComparingTo("10000");
    assertThat(cartItem.getUnitPrice()).isEqualByComparingTo("8000");
    assertThat(cartItem.getSubtotalPrice()).isEqualByComparingTo("16000");
    assertThat(cartItem.getLastAppliedDishPriceVersion()).isEqualTo(2L);
  }

  @Test
  void 정가가_변경되어도_장바구니에_담을_때의_정가를_보존한다() {
    // given
    CartItem cartItem =
        cartItem("김치찌개", BigDecimal.valueOf(10_000), BigDecimal.valueOf(8_000), 2L, 1L);

    // when
    cartItem.synchronizeDishPrice(BigDecimal.valueOf(12_000), BigDecimal.valueOf(8_000), 2L);

    // then
    assertThat(cartItem.getDishPrice()).isEqualByComparingTo("10000");
    assertThat(cartItem.getUnitPrice()).isEqualByComparingTo("8000");
  }

  @Test
  void 이전_Dish_가격_이벤트는_무시한다() {
    // given
    CartItem cartItem = cartItem("김치찌개", BigDecimal.valueOf(8_000), 2L, 2L);

    // when
    cartItem.synchronizeDishPrice(BigDecimal.valueOf(8_000), BigDecimal.valueOf(6_000), 1L);

    // then
    assertThat(cartItem.getUnitPrice()).isEqualByComparingTo("8000");
    assertThat(cartItem.getLastAppliedDishPriceVersion()).isEqualTo(2L);
  }

  @Test
  void 가격_이벤트의_값과_무관하게_장바구니_가격을_보존한다() {
    // given
    CartItem cartItem = cartItem("김치찌개", BigDecimal.valueOf(8_000), 2L, 1L);

    // when
    cartItem.synchronizeDishPrice(BigDecimal.valueOf(8_000), BigDecimal.valueOf(-1), 2L);

    // then
    assertThat(cartItem.getUnitPrice()).isEqualByComparingTo("8000");
    assertThat(cartItem.getLastAppliedDishPriceVersion()).isEqualTo(2L);
  }

  @Test
  void 가격_이벤트에서_정가가_판매가보다_낮아도_장바구니_가격을_보존한다() {
    // given
    CartItem cartItem = cartItem("김치찌개", BigDecimal.valueOf(8_000), 2L, 1L);

    // when
    cartItem.synchronizeDishPrice(BigDecimal.valueOf(5_000), BigDecimal.valueOf(7_000), 2L);

    // then
    assertThat(cartItem.getDishPrice()).isEqualByComparingTo("8000");
    assertThat(cartItem.getUnitPrice()).isEqualByComparingTo("8000");
  }

  @Test
  void 상품_추가시_정가와_판매가를_모두_저장한다() {
    CartItem cartItem =
        cartItem("김치찌개", BigDecimal.valueOf(10_000), BigDecimal.valueOf(7_000), 3L, 0L);

    assertThat(cartItem.getDishPrice()).isEqualByComparingTo("10000");
    assertThat(cartItem.getUnitPrice()).isEqualByComparingTo("7000");
    assertThat(cartItem.getSubtotalPrice()).isEqualByComparingTo("21000");
  }
}
