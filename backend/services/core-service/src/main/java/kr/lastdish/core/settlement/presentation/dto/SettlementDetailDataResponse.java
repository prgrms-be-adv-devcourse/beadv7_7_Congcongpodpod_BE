package kr.lastdish.core.settlement.presentation.dto;

import kr.lastdish.core.settlement.domain.SettlementDetail;

public record SettlementDetailDataResponse(
    Long settlementDetailId,
    Long orderId,
    long salesAmount,
    long feeAmount,
    long settlementAmount) {

  public static SettlementDetailDataResponse from(SettlementDetail detail) {
    return new SettlementDetailDataResponse(
        detail.getId(),
        detail.getOrderId(),
        detail.getSalesAmount(),
        detail.getFeeAmount(),
        detail.getSettlementAmount());
  }
}
