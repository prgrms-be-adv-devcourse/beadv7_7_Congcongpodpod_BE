package kr.lastdish.core.cart.presentation.dto;

import java.math.BigDecimal;
import kr.lastdish.core.cart.domain.CartItem;

public record CartItemResponse(
    Long cartItemId,
    Long dishId,
    String dishName,
    BigDecimal unitPrice,
    Long quantity,
    BigDecimal subtotalPrice) {

  public static CartItemResponse from(CartItem cartItem) {
    return new CartItemResponse(
        cartItem.getId(),
        cartItem.getDishId(),
        cartItem.getDishName(),
        cartItem.getUnitPrice(),
        cartItem.getQuantity(),
        cartItem.getSubtotalPrice());
  }
}
