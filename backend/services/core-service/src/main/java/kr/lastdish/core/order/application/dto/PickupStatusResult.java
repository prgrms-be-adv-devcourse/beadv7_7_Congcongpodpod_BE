package kr.lastdish.core.order.application.dto;

import kr.lastdish.core.order.domain.Order;
import kr.lastdish.core.order.domain.OrderStatus;

public record PickupStatusResult(Long orderId, OrderStatus status) {
  public static PickupStatusResult from(Order order) {
    return new PickupStatusResult(order.getId(), order.getStatus());
  }
}
