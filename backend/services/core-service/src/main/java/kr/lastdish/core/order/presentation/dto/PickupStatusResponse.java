package kr.lastdish.core.order.presentation.dto;

import kr.lastdish.core.order.application.dto.PickupStatusResult;
import kr.lastdish.core.order.domain.OrderStatus;

public record PickupStatusResponse(Long orderId, OrderStatus status) {
  public static PickupStatusResponse from(PickupStatusResult result) {
    return new PickupStatusResponse(result.orderId(), result.status());
  }
}
