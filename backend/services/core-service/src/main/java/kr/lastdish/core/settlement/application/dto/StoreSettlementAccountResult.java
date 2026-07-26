package kr.lastdish.core.settlement.application.dto;

public record StoreSettlementAccountResult(
    String bankName, String accountNumber, String accountHolder) {}
