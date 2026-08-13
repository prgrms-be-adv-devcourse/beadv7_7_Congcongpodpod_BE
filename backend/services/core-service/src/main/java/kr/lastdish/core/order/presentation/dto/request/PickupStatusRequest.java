package kr.lastdish.core.order.presentation.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import kr.lastdish.core.order.application.dto.UpdatePickupStatusCommand;
import kr.lastdish.core.order.domain.OrderStatus;

public record PickupStatusRequest(@NotNull OrderStatus status) {
  @AssertTrue(message = "픽업 상태는 PICKED_UP 또는 NO_SHOW만 가능합니다.")
  public boolean isValidStatus() {
    return status == null || status == OrderStatus.PICKED_UP || status == OrderStatus.NO_SHOW;
  }

  public UpdatePickupStatusCommand toCommand() {
    return new UpdatePickupStatusCommand(status);
  }
}
