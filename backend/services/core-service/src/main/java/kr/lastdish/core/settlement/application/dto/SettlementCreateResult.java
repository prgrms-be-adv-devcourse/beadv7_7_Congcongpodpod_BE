package kr.lastdish.core.settlement.application.dto;

public record SettlementCreateResult(
    Long storeId, Long settlementId, int orderCount, boolean created) {
  public static SettlementCreateResult created(Long storeId, Long settlementId, int orderCount) {
    return new SettlementCreateResult(storeId, settlementId, orderCount, true);
  }

  public static SettlementCreateResult skipped(Long storeId) {
    return new SettlementCreateResult(storeId, null, 0, false);
  }
}
