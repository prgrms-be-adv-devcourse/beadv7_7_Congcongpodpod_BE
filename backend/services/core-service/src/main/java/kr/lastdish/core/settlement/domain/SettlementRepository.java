package kr.lastdish.core.settlement.domain;

import java.time.YearMonth;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SettlementRepository {
  Settlement save(Settlement settlement);

  Optional<Settlement> findById(Long settlementId);

  Optional<Settlement> findByStoreIdAndSettlementMonth(Long storeId, YearMonth settlementMonth);

  boolean existsByStoreIdAndSettlementMonth(Long storeId, YearMonth settlementMonth);

  boolean existsByStoreIdAndSettlementMonthAndSettlementStatus(
      Long storeId, YearMonth settlementMonth, SettlementStatus settlementStatus);

  Page<Settlement> findAllByStoreId(Long storeId, Pageable pageable);

  Optional<Settlement> findByIdAndStoreId(Long settlementId, Long storeId);
}
