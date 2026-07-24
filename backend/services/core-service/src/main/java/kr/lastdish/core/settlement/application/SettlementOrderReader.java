package kr.lastdish.core.settlement.application;

import java.time.LocalDateTime;
import java.util.List;
import kr.lastdish.core.settlement.application.dto.SettlementOrderData;

public interface SettlementOrderReader {
  List<SettlementOrderData> readSettlementOrders(
      Long storeId, LocalDateTime periodStart, LocalDateTime periodEnd);
}
