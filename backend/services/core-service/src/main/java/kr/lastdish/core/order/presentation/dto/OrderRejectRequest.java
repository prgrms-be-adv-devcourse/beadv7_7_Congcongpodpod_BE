package kr.lastdish.core.order.presentation.dto;

import jakarta.validation.constraints.NotNull;

public record OrderRejectRequest(@NotNull OrderRejectReason reason) {}
