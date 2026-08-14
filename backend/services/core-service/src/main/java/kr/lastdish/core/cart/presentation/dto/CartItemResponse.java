package kr.lastdish.core.cart.presentation.dto;

import java.math.BigDecimal;
import kr.lastdish.core.cart.domain.CartItem;
import kr.lastdish.core.cart.domain.CartItemStatus;

public record CartItemResponse(
    // 참고: status는 화면에 이유를 표시할 때 사용하고, orderable은 주문 버튼 활성화 여부에 사용합니다.
    Long cartItemId,
    Long dishId,
    String dishName,
    BigDecimal unitPrice,
    Long quantity,
    BigDecimal subtotalPrice,
    CartItemStatus status,
    boolean orderable) {

  public static CartItemResponse from(CartItem cartItem) {
    return new CartItemResponse(
        cartItem.getId(),
        cartItem.getDishId(),
        cartItem.getDishName(),
        cartItem.getUnitPrice(),
        cartItem.getQuantity(),
        cartItem.getSubtotalPrice(),
        cartItem.getStatus(),
        cartItem.isOrderable());
  }
}
