package kr.lastdish.core.order.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import kr.lastdish.core.order.domain.OrderSettlementTarget;

public record OrderSettlementInfo(
    Long orderId, Long storeId, BigDecimal salesAmount, LocalDateTime orderCompletedAt) {
  public static OrderSettlementInfo from(OrderSettlementTarget target) {
    return new OrderSettlementInfo(
        target.id(), target.storeId(), target.totalPrice(), target.updatedAt());
  }
}
