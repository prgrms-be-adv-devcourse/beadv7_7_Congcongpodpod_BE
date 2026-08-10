package kr.lastdish.core.order.presentation.dto;

import jakarta.validation.constraints.NotNull;
import kr.lastdish.core.order.application.dto.UpdatePickupStatusCommand;
import kr.lastdish.core.order.domain.OrderStatus;

public record PickupStatusRequest(@NotNull OrderStatus status) {
  public UpdatePickupStatusCommand toCommand() {
    return new UpdatePickupStatusCommand(status);
  }
}
