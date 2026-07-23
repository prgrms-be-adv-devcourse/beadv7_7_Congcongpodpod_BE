package kr.lastdish.core.settlement.application;

import kr.lastdish.core.settlement.application.dto.SettlementOrderData;

import java.time.LocalDateTime;
import java.util.List;

public interface SettlementOrderReader {
    List<SettlementOrderData> readSettlementOrders(
            Long storeId,
            LocalDateTime periodStart,
            LocalDateTime periodEnd
    );
}
