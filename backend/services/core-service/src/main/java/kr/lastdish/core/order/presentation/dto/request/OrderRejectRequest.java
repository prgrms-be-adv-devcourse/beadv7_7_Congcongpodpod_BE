package kr.lastdish.core.order.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import kr.lastdish.core.order.application.dto.RejectOrderCommand;
import kr.lastdish.core.order.domain.OrderRejectReason;

public record OrderRejectRequest(@NotNull OrderRejectReason reason) {
  public RejectOrderCommand toCommand() {
    return new RejectOrderCommand(reason);
  }
}
