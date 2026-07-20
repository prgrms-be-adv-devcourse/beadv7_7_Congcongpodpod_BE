package kr.lastdish.core.payment.application.dto;

import kr.lastdish.core.payment.domain.deposit.DepositHistory;
import java.math.BigDecimal;

public record DepositUseResult(
        Long depositHistoryId,
        BigDecimal usedAmount,
        BigDecimal balanceAfter
) {
    public static DepositUseResult from(DepositHistory history) {
        return new DepositUseResult(
                history.getId(),
                history.getAmount(),
                history.getBalanceAfter()
        );
    }
}
