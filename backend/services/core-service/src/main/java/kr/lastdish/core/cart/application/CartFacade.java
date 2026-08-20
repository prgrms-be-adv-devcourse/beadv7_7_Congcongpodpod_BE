package kr.lastdish.core.cart.application;

import kr.lastdish.core.cart.application.dto.CartOrderSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CartFacade {
  private final CartService cartService;

  public CartOrderSnapshot getValidatedOrderSnapshot(
      Long memberId, Long cartItemId, long expectedDishPriceVersion) {
    return cartService.getValidatedOrderSnapshot(memberId, cartItemId, expectedDishPriceVersion);
  }

  public void removeOrderedItem(Long memberId, Long cartItemId) {
    cartService.removeOrderedItem(memberId, cartItemId);
  }
}
