package kr.lastdish.core.settlement.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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

  Set<Long> findSettledStoreIds(List<Long> storeIds, YearMonth settlementMonth);

  void insertAccumulatingIfAbsent(
      Long storeId,
      YearMonth settlementMonth,
      LocalDateTime periodStart,
      LocalDateTime periodEnd,
      BigDecimal feeRate);

  List<Long> findTargetStoreIds(YearMonth settlementMonth, Set<SettlementStatus> statuses);
}
