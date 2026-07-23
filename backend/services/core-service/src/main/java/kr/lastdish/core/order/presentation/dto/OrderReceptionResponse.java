package kr.lastdish.core.order.presentation.dto;

import kr.lastdish.core.order.domain.Order;

public record OrderReceptionResponse(Long orderId, String pickUpCode) {
  public static OrderReceptionResponse from(Order order) {
    return new OrderReceptionResponse(order.getId(), order.getPickupCode());
  }
}
