package kr.lastdish.core.payment.application.dto;

import kr.lastdish.core.payment.domain.deposit.Deposit;

import java.math.BigDecimal;

public record DepositBalanceResponse(
        Long memberId,
        BigDecimal balance
) {
    public static DepositBalanceResponse from(Deposit deposit) {
        return new DepositBalanceResponse(deposit.getMemberId(), deposit.getBalance());
    }
}