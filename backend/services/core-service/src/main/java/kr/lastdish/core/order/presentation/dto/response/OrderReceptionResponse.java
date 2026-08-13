package kr.lastdish.core.order.presentation.dto.response;

import kr.lastdish.core.order.application.dto.OrderReceptionResult;

public record OrderReceptionResponse(Long orderId, String pickUpCode) {
  public static OrderReceptionResponse from(OrderReceptionResult result) {
    return new OrderReceptionResponse(result.orderId(), result.pickUpCode());
  }
}
