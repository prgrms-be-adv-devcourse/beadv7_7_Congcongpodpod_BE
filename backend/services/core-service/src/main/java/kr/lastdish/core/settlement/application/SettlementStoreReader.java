package kr.lastdish.core.settlement.application;

import kr.lastdish.core.settlement.application.dto.SettlementAccountData;

import java.util.List;
import java.util.Optional;

public interface SettlementStoreReader {
  List<Long> readSettlementTargetStoreIds();
  Optional<SettlementAccountData> readAccountByStoreId(Long storeId);
}
