package kr.lastdish.core.cart.presentation.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import kr.lastdish.core.cart.domain.CartItem;
import kr.lastdish.core.cart.domain.CartItemStatus;
import org.junit.jupiter.api.Test;

class CartItemResponseTest {

  @Test
  void CartItem의_주문가능_상태를_응답에_포함한다() {
    // given
    CartItem cartItem = CartItem.create(1L, 10L, "김치찌개", BigDecimal.valueOf(8_000), 7L);

    cartItem.synchronizeDishState(true, 5L, 1L);

    // when
    CartItemResponse response = CartItemResponse.from(cartItem);

    // then
    assertThat(response.status()).isEqualTo(CartItemStatus.INSUFFICIENT_STOCK);

    assertThat(response.orderable()).isFalse();
  }
}
