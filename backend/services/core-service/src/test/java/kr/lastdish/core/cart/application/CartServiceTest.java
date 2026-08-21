package kr.lastdish.core.cart.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Optional;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.core.cart.application.dto.CartOrderSnapshot;
import kr.lastdish.core.cart.domain.Cart;
import kr.lastdish.core.cart.domain.CartItem;
import kr.lastdish.core.cart.domain.CartItemRepository;
import kr.lastdish.core.cart.domain.CartRepository;
import kr.lastdish.core.common.exception.ErrorCode;
import kr.lastdish.core.dish.application.DishFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

  @Mock private CartRepository cartRepository;
  @Mock private CartItemRepository cartItemRepository;
  @Mock private DishFacade dishFacade;
  @InjectMocks private CartService cartService;

  @Test
  void Dish_가격_버전이_일치하고_주문_가능하면_검증된_스냅샷을_반환한다() {
    Long memberId = 1L;
    Long cartId = 2L;
    Long cartItemId = 3L;
    CartItem cartItem = mockOrderCartItem(memberId, cartId, cartItemId, 7L, true);
    when(cartItem.getStoreId()).thenReturn(10L);
    when(cartItem.getDishId()).thenReturn(20L);
    when(cartItem.getDishName()).thenReturn("김밥");
    when(cartItem.getQuantity()).thenReturn(2L);
    when(cartItem.getDishPrice()).thenReturn(BigDecimal.valueOf(6000));
    when(cartItem.getUnitPrice()).thenReturn(BigDecimal.valueOf(5000));
    when(cartItem.getPickupStartAt()).thenReturn(LocalTime.of(18, 0));
    when(cartItem.getPickupEndAt()).thenReturn(LocalTime.of(19, 0));

    CartOrderSnapshot snapshot = cartService.getValidatedOrderSnapshot(memberId, cartItemId, 7L);

    assertThat(snapshot.dishId()).isEqualTo(20L);
    assertThat(snapshot.quantity()).isEqualTo(2L);
    assertThat(snapshot.unitPrice()).isEqualByComparingTo("5000");
  }

  @Test
  void Dish_가격_버전이_다르면_스냅샷을_반환하지_않는다() {
    Long memberId = 1L;
    Long cartItemId = 3L;
    mockOrderCartItem(memberId, 2L, cartItemId, 7L, true);

    assertThatThrownBy(() -> cartService.getValidatedOrderSnapshot(memberId, cartItemId, 6L))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.ORDER_DISH_PRICE_CHANGED);
  }

  @Test
  void Dish_가격_버전이_일치해도_CartItem이_주문_불가하면_스냅샷을_반환하지_않는다() {
    Long memberId = 1L;
    Long cartItemId = 3L;
    mockOrderCartItem(memberId, 2L, cartItemId, 7L, false);

    assertThatThrownBy(() -> cartService.getValidatedOrderSnapshot(memberId, cartItemId, 7L))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.CART_ITEM_NOT_ORDERABLE);
  }

  @Test
  void removeOrderedItem_deletesOwnedCartItem() {
    Long memberId = 1L;
    Long cartId = 2L;
    Long cartItemId = 3L;
    Cart cart = Cart.create(memberId);
    CartItem cartItem = org.mockito.Mockito.mock(CartItem.class);

    when(cartItemRepository.findById(cartItemId)).thenReturn(Optional.of(cartItem));
    when(cartItem.getCartId()).thenReturn(cartId);
    when(cartRepository.findById(cartId)).thenReturn(Optional.of(cart));

    cartService.removeOrderedItem(memberId, cartItemId);

    verify(cartItemRepository).delete(cartItem);
  }

  private CartItem mockOrderCartItem(
      Long memberId,
      Long cartId,
      Long cartItemId,
      long lastAppliedDishPriceVersion,
      boolean orderable) {
    Cart cart = Cart.create(memberId);
    CartItem cartItem = org.mockito.Mockito.mock(CartItem.class);

    when(cartItemRepository.findWithLockById(cartItemId)).thenReturn(Optional.of(cartItem));
    when(cartItem.getCartId()).thenReturn(cartId);
    when(cartRepository.findById(cartId)).thenReturn(Optional.of(cart));
    when(cartItem.getLastAppliedDishPriceVersion()).thenReturn(lastAppliedDishPriceVersion);
    lenient().when(cartItem.isOrderable()).thenReturn(orderable);
    return cartItem;
  }
}
