package kr.lastdish.core.order.application.dto;

import kr.lastdish.core.order.domain.OrderStatus;

public record UpdatePickupStatusCommand(OrderStatus status) {
  public UpdatePickupStatusCommand {
    if (status != OrderStatus.PICKED_UP && status != OrderStatus.NO_SHOW) {
      throw new IllegalArgumentException("픽업 상태는 PICKED_UP 또는 NO_SHOW만 가능합니다.");
    }
  }
}
