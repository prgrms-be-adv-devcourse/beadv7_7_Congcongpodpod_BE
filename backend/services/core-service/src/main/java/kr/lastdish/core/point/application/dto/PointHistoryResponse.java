package kr.lastdish.core.point.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import kr.lastdish.core.point.domain.PointHistory;
import kr.lastdish.core.point.domain.PointType;

public record PointHistoryResponse(
    Long historyId,
    Long orderId,
    PointType type,
    BigDecimal amount,
    BigDecimal balanceAfter,
    LocalDateTime expiresAt,
    LocalDateTime createdAt) {

  public static PointHistoryResponse from(PointHistory history) {
    return new PointHistoryResponse(
        history.getId(),
        history.getOrderId(),
        history.getType(),
        history.getAmount(),
        history.getBalanceAfter(),
        history.getExpiresAt(),
        history.getCreatedAt());
  }
}
