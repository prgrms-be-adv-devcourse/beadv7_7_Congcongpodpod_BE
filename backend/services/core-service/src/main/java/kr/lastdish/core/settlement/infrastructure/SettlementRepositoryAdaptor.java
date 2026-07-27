package kr.lastdish.core.settlement.infrastructure;

import java.time.YearMonth;
import java.util.Optional;
import kr.lastdish.core.settlement.domain.Settlement;
import kr.lastdish.core.settlement.domain.SettlementRepository;
import kr.lastdish.core.settlement.domain.SettlementStatus;
import lombok.RequiredArgsConstructor;
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
}
