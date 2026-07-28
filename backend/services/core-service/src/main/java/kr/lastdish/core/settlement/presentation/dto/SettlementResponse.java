package kr.lastdish.core.settlement.presentation.dto;

import java.time.YearMonth;
import kr.lastdish.core.settlement.domain.Settlement;
import kr.lastdish.core.settlement.domain.SettlementStatus;

public record SettlementResponse(
    Long settlementId,
    Long storeId,
    YearMonth settlementMonth,
    long orderCount,
    long grossAmount,
    long feeAmount,
    long settlementAmount,
    SettlementStatus status) {

  public static SettlementResponse from(Settlement settlement) {
    return new SettlementResponse(
        settlement.getId(),
        settlement.getStoreId(),
        settlement.getSettlementMonth(),
        settlement.getTotalOrderCount(),
        settlement.getGrossAmount(),
        settlement.getFeeAmount(),
        settlement.getSettlementAmount(),
        settlement.getSettlementStatus());
  }
}
