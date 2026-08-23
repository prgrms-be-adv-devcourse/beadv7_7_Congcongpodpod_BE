package kr.lastdish.core.settlement.domain;

public record SettlementDetailSummary(
        long totalOrderCount,
        long grossAmount,
        long feeAmount,
        long settlementAmount
) {}
