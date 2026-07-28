package kr.lastdish.core.cart.application;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import kr.lastdish.core.cart.domain.CartItem;
import kr.lastdish.core.cart.domain.CartItemRepository;
import org.junit.jupiter.api.Test;

class CartDishPriceSynchronizerTest {

  @Test
  void 같은_Dish를_담은_모든_CartItem에_가격을_반영한다() {
    // given
    CartItemRepository repository = mock(CartItemRepository.class);
    CartItem first = mock(CartItem.class);
    CartItem second = mock(CartItem.class);
    CartDishPriceSynchronizer synchronizer = new CartDishPriceSynchronizer(repository);
    BigDecimal unitPrice = BigDecimal.valueOf(7_000);

    when(repository.findAllByDishId(10L)).thenReturn(List.of(first, second));

    // when
    synchronizer.synchronize(10L, unitPrice, 3L);

    // then
    verify(first).synchronizeDishPrice(unitPrice, 3L);
    verify(second).synchronizeDishPrice(unitPrice, 3L);
  }
}
