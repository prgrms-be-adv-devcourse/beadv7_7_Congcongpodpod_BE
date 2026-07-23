package kr.lastdish.core.order.presentation.dto;

import kr.lastdish.core.order.domain.Order;
import kr.lastdish.core.order.domain.OrderStatus;

public record PickupStatusResponse(Long orderId, OrderStatus status) {
  public static PickupStatusResponse from(Order order) {
    return new PickupStatusResponse(order.getId(), order.getStatus());
  }
}
