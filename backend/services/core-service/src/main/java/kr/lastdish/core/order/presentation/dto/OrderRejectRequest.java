package kr.lastdish.core.order.presentation.dto;

import jakarta.validation.constraints.NotNull;
import kr.lastdish.core.order.domain.OrderRejectReason;

public record OrderRejectRequest(@NotNull OrderRejectReason reason) {}
