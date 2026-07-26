package kr.lastdish.core.settlement.application;

import java.util.List;
import java.util.Optional;
import kr.lastdish.core.settlement.application.dto.SettlementAccountData;

public interface SettlementStoreReader {
  List<Long> readSettlementTargetStoreIds();

  Optional<SettlementAccountData> readAccountByStoreId(Long storeId);
}
