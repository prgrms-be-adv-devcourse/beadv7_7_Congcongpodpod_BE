package kr.lastdish.core.settlement.application.dto;

/*
  매장별 정산 결과 출력용 객체
 */
public record SettlementProcessResult(
        Long storeId,
        Long settlementId,
        SettlementProcessStatus status,
        String message
) {
    public static SettlementProcessResult created(Long storeId, Long settlementId){
         return new SettlementProcessResult(storeId, settlementId, SettlementProcessStatus.CREATED, null);
    }

    public static SettlementProcessResult retried(Long storeId, Long settlementId){
        return new SettlementProcessResult(storeId, settlementId, SettlementProcessStatus.RETRIED, null);
    }

    public static SettlementProcessResult skipped(Long storeId, Long settlementId, String message){
        return new SettlementProcessResult(storeId, settlementId, SettlementProcessStatus.SKIPPED, message);
    }

    public static SettlementProcessResult failed(Long storeId, Long settlementId, String message){
        return new SettlementProcessResult(storeId, settlementId, SettlementProcessStatus.FAILED, message);
    }
}
