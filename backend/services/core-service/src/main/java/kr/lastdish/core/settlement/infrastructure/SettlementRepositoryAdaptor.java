package kr.lastdish.core.settlement.infrastructure;

import java.time.YearMonth;
import java.util.*;
import kr.lastdish.core.settlement.domain.Settlement;
import kr.lastdish.core.settlement.domain.SettlementRepository;
import kr.lastdish.core.settlement.domain.SettlementStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SettlementRepositoryAdaptor implements SettlementRepository {
  private final JpaSettlementRepository jpaSettlementRepository;

  @Override
  public Settlement save(Settlement settlement) {
    return jpaSettlementRepository.save(settlement);
  }

  @Override
  public Optional<Settlement> findById(Long settlementId) {
    return jpaSettlementRepository.findById(settlementId);
  }

  @Override
  public Optional<Settlement> findByStoreIdAndSettlementMonth(
      Long storeId, YearMonth settlementMonth) {
    return jpaSettlementRepository.findByStoreIdAndSettlementMonth(storeId, settlementMonth);
  }

  @Override
  public boolean existsByStoreIdAndSettlementMonth(Long storeId, YearMonth settlementMonth) {
    return jpaSettlementRepository.existsByStoreIdAndSettlementMonth(storeId, settlementMonth);
  }

  @Override
  public boolean existsByStoreIdAndSettlementMonthAndSettlementStatus(
      Long storeId, YearMonth settlementMonth, SettlementStatus settlementStatus) {
    return jpaSettlementRepository.existsByStoreIdAndSettlementMonthAndSettlementStatus(
        storeId, settlementMonth, settlementStatus);
  }

  @Override
  public Page<Settlement> findAllByStoreId(Long storeId, Pageable pageable) {
    return jpaSettlementRepository.findAllByStoreIdOrderBySettlementMonthDesc(storeId, pageable);
  }

  @Override
  public Optional<Settlement> findByIdAndStoreId(Long settlementId, Long storeId) {
    return jpaSettlementRepository.findByIdAndStoreId(settlementId, storeId);
  }

  @Override
  public Set<Long> findSettledStoreIds(List<Long> storeIds) {
    if (storeIds == null || storeIds.isEmpty()) {
      return Set.of();
    }

    return new HashSet<>(jpaSettlementRepository.findSettledStoreIds(storeIds));
  }

  @Override
  public void truncateAllSettlementData() {
    jpaSettlementRepository.truncateAllSettlementData();
  }
}
