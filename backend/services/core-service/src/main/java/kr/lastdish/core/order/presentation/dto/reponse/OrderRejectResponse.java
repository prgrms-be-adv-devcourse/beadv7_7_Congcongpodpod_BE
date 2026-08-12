package kr.lastdish.core.order.presentation.dto.reponse;

import kr.lastdish.core.order.application.dto.OrderRejectResult;
import kr.lastdish.core.order.domain.OrderStatus;

public record OrderRejectResponse(Long orderId, OrderStatus status, String rejectReason) {
  public static OrderRejectResponse from(OrderRejectResult result) {
    return new OrderRejectResponse(result.orderId(), result.status(), result.rejectReason());
  }
}
