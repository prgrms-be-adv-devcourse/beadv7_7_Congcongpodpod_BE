package kr.lastdish.core.settlement.domain;

public interface SettlementDetailSummaryProjection {
  Long getTotalOrderCount();

  Long getGrossAmount();

  Long getFeeAmount();

  Long getSettlementAmount();
}
