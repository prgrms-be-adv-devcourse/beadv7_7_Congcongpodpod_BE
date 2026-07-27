package kr.lastdish.core.settlement.domain;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface SettlementDetailRepository {
  SettlementDetail save(SettlementDetail settlementDetail);

  List<SettlementDetail> saveAll(List<SettlementDetail> settlementDetails);

  boolean existsByOrderId(Long orderId);

  Set<Long> findSettledOrderIds(Collection<Long> orderIds);
}
