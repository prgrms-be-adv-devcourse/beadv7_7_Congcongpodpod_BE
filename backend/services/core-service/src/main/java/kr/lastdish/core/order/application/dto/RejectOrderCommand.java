package kr.lastdish.core.order.application.dto;

import kr.lastdish.core.order.domain.OrderRejectReason;

public record RejectOrderCommand(OrderRejectReason reason) {}
