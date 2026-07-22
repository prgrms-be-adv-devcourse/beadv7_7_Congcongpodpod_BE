package kr.lastdish.core.cart.presentation.dto;

import java.math.BigDecimal;
import java.util.List;
import kr.lastdish.core.cart.domain.Cart;
import kr.lastdish.core.cart.domain.CartItem;

public record CartResponse(
    Long cartId, Long memberId, List<CartItemResponse> items, BigDecimal totalPrice) {

  // 장바구니 1개 = 상품 1개라 items는 현재 0건 또는 1건이지만,
  // 이후 다중 상품으로 확장될 때 API 응답 구조를 바꾸지 않기 위해 List로 둔다.
  public static CartResponse of(Cart cart, List<CartItem> items) {
    List<CartItemResponse> itemResponses = items.stream().map(CartItemResponse::from).toList();
    BigDecimal totalPrice =
        items.stream().map(CartItem::getSubtotalPrice).reduce(BigDecimal.ZERO, BigDecimal::add);

    return new CartResponse(cart.getId(), cart.getMemberId(), itemResponses, totalPrice);
  }
}
