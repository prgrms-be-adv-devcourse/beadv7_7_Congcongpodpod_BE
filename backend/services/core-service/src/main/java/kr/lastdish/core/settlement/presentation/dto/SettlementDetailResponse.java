package kr.lastdish.core.settlement.presentation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import kr.lastdish.core.settlement.domain.Settlement;
import kr.lastdish.core.settlement.domain.SettlementDetail;
import kr.lastdish.core.settlement.domain.SettlementStatus;

public record SettlementDetailResponse(
    Long settlementId,
    Long storeId,
    YearMonth settlementMonth,
    LocalDateTime periodStart,
    LocalDateTime periodEnd,
    long orderCount,
    long grossAmount,
    BigDecimal feeRate,
    long feeAmount,
    long settlementAmount,
    SettlementStatus status,
    String failureReason,
    String bankName,
    String accountNumber,
    String accountHolder,
    List<SettlementDetailDataResponse> orders) {

  public static SettlementDetailResponse of(Settlement settlement, List<SettlementDetail> details) {
    return new SettlementDetailResponse(
        settlement.getId(),
        settlement.getStoreId(),
        settlement.getSettlementMonth(),
        settlement.getPeriodStart(),
        settlement.getPeriodEnd(),
        settlement.getTotalOrderCount(),
        settlement.getGrossAmount(),
        settlement.getFeeRate(),
        settlement.getFeeAmount(),
        settlement.getSettlementAmount(),
        settlement.getSettlementStatus(),
        settlement.getFailureReason(),
        settlement.getBankCode(),
        settlement.getAccountNumber(),
        settlement.getAccountHolder(),
        details.stream().map(SettlementDetailDataResponse::from).toList());
  }
}
