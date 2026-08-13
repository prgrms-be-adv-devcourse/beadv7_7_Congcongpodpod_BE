package kr.lastdish.core.settlement.application.dto;

import java.math.BigDecimal;

public record OrderSettlementAmount(
    SettlementOrderData order,
    long salesAmount,
    BigDecimal feeRate,
    long feeAmount,
    long settlementAmount) {}
