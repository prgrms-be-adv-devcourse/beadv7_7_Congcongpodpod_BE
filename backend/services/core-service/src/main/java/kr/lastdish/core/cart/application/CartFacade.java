package kr.lastdish.core.cart.application;

import kr.lastdish.core.cart.application.dto.CartOrderSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CartFacade {
  private final CartService cartService;

  public CartOrderSnapshot getOrderSnapshot(Long memberId, Long cartItemId) {
    return cartService.getOrderSnapshot(memberId, cartItemId);
  }
}
