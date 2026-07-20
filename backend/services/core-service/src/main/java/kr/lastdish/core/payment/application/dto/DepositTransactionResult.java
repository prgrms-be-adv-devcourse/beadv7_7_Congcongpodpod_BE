package kr.lastdish.core.payment.application.dto;

import java.math.BigDecimal;
import kr.lastdish.core.payment.domain.deposit.DepositHistory;

public record DepositTransactionResult(
    Long depositHistoryId, BigDecimal amount, BigDecimal balanceAfter) {
  public static DepositTransactionResult from(DepositHistory history) {

    return new DepositTransactionResult(
        history.getId(), history.getAmount(), history.getBalanceAfter());
  }
}
