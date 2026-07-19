package kr.lastdish.core.payment.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import kr.lastdish.core.payment.domain.deposit.DepositHistory;

public record DepositHistoryResponse(
    Long id,
    Long orderId,
    Long paymentId,
    DepositHistory.DepositType type,
    BigDecimal amount,
    BigDecimal balanceAfter,
    LocalDateTime createdAt) {
  public static DepositHistoryResponse from(DepositHistory history) {
    return new DepositHistoryResponse(
        history.getId(),
        history.getOrderId(),
        history.getPaymentId(),
        history.getType(),
        history.getAmount(),
        history.getBalanceAfter(),
        history.getCreatedAt());
  }
}
