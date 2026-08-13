package kr.lastdish.core.order.domain.event;

import kr.lastdish.core.order.domain.OrderStatus;

public record OrderStatusChangedPayload(Long memberId, OrderStatus status) {}
