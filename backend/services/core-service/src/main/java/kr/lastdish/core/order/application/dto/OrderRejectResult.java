package kr.lastdish.core.order.application.dto;

import kr.lastdish.core.order.domain.Order;
import kr.lastdish.core.order.domain.OrderStatus;

public record OrderRejectResult(Long orderId, OrderStatus status, String rejectReason) {
  public static OrderRejectResult from(Order order) {
    return new OrderRejectResult(
        order.getId(),
        order.getStatus(),
        order.getRejectReason() == null ? null : order.getRejectReason().getMessage());
  }
}
