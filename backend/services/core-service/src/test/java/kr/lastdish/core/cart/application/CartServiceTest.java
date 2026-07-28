package kr.lastdish.core.cart.application;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import kr.lastdish.core.cart.domain.Cart;
import kr.lastdish.core.cart.domain.CartItem;
import kr.lastdish.core.cart.domain.CartItemRepository;
import kr.lastdish.core.cart.domain.CartRepository;
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
}
