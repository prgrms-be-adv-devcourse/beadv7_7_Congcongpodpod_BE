package kr.lastdish.core.settlement.application.dto;

public record SettlementAccountData(
        String bankName,
        String accountNumber,
        String accountHolder
) {
}
