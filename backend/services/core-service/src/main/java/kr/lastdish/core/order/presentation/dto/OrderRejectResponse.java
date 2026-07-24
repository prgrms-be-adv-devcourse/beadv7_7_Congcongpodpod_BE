package kr.lastdish.core.order.presentation.dto;

import kr.lastdish.core.order.domain.Order;
import kr.lastdish.core.order.domain.OrderStatus;

public record OrderRejectResponse(Long orderId, OrderStatus status, String rejectReason) {
  public static OrderRejectResponse from(Order order) {
    return new OrderRejectResponse(
        order.getId(),
        order.getStatus(),
        order.getRejectReason() == null ? null : order.getRejectReason().getMessage());
  }
}
