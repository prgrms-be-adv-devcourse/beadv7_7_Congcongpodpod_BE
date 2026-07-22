package kr.lastdish.core.cart.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import kr.lastdish.core.cart.domain.CartItem;
import kr.lastdish.core.cart.domain.CartItemRepository;
import kr.lastdish.core.cart.domain.CartItemStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CartDishStateSynchronizerTest {

  @Mock private CartItemRepository cartItemRepository;

  private CartDishStateSynchronizer synchronizer;

  @BeforeEach
  void setUp() {
    synchronizer = new CartDishStateSynchronizer(cartItemRepository);
  }

  @Test
  void Dish_재고를_기준으로_모든_CartItem_상태를_갱신한다() {
    // given
    CartItem orderableItem = CartItem.create(1L, 10L, "김치찌개", BigDecimal.valueOf(8_000), 3L);

    CartItem insufficientItem = CartItem.create(2L, 10L, "김치찌개", BigDecimal.valueOf(8_000), 7L);

    when(cartItemRepository.findAllByDishId(10L))
        .thenReturn(List.of(orderableItem, insufficientItem));

    // when
    synchronizer.synchronize(10L, true, 5L);

    // then
    verify(cartItemRepository).findAllByDishId(10L);

    assertThat(orderableItem.getStatus()).isEqualTo(CartItemStatus.AVAILABLE);

    assertThat(insufficientItem.getStatus()).isEqualTo(CartItemStatus.INSUFFICIENT_STOCK);
  }

  @Test
  void Dish가_판매불가이면_관련_CartItem을_판매불가로_변경한다() {
    // given
    CartItem cartItem = CartItem.create(1L, 10L, "김치찌개", BigDecimal.valueOf(8_000), 1L);

    when(cartItemRepository.findAllByDishId(10L)).thenReturn(List.of(cartItem));

    // when
    synchronizer.synchronize(10L, false, 10L);

    // then
    assertThat(cartItem.getStatus()).isEqualTo(CartItemStatus.DISH_UNAVAILABLE);

    assertThat(cartItem.isOrderable()).isFalse();
  }
}
