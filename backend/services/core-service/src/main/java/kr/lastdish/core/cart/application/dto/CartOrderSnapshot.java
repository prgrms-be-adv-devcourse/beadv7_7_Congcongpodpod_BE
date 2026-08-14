package kr.lastdish.core.cart.application.dto;

import java.math.BigDecimal;
import java.time.LocalTime;
import kr.lastdish.core.cart.domain.CartItem;

public record CartOrderSnapshot(
    Long storeId,
    Long dishId,
    String dishName,
    Long quantity,
    BigDecimal unitPrice,
    BigDecimal totalPrice,
    BigDecimal savedAmount,
    LocalTime pickupStartAt,
    LocalTime pickupEndAt) {

  public CartOrderSnapshot(
      Long storeId,
      Long dishId,
      String dishName,
      Long quantity,
      BigDecimal unitPrice,
      LocalTime pickupStartAt,
      LocalTime pickupEndAt) {
    this(
        storeId,
        dishId,
        dishName,
        quantity,
        unitPrice,
        unitPrice.multiply(BigDecimal.valueOf(quantity)),
        BigDecimal.ZERO,
        pickupStartAt,
        pickupEndAt);
  }

  public static CartOrderSnapshot from(CartItem cartItem) {
    return new CartOrderSnapshot(
        cartItem.getStoreId(),
        cartItem.getDishId(),
        cartItem.getDishName(),
        cartItem.getQuantity(),
        cartItem.getUnitPrice(),
        cartItem.getTotalPrice(),
        cartItem.getSavedAmount(),
        cartItem.getPickupStartAt(),
        cartItem.getPickupEndAt());
  }
}
