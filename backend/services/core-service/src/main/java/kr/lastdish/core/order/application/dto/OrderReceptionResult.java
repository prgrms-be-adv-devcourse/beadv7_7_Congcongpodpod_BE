package kr.lastdish.core.order.application.dto;

import kr.lastdish.core.order.domain.Order;

public record OrderReceptionResult(Long orderId, String pickUpCode) {
  public static OrderReceptionResult from(Order order) {
    return new OrderReceptionResult(order.getId(), order.getPickupCode());
  }
}
