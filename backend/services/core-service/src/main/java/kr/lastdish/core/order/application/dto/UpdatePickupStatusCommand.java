package kr.lastdish.core.order.application.dto;

import kr.lastdish.core.order.domain.OrderStatus;

public record UpdatePickupStatusCommand(OrderStatus status) {}
