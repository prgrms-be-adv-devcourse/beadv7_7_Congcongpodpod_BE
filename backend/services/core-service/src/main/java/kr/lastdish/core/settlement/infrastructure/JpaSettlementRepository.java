package kr.lastdish.core.settlement.infrastructure;

import kr.lastdish.core.settlement.domain.Settlement;
import kr.lastdish.core.settlement.domain.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.YearMonth;
import java.util.Optional;

public interface JpaSettlementRepository extends JpaRepository<Settlement, Long> {
    boolean existsByStoreIdAndSettlementMonth(Long storeId, YearMonth settlementMonth);

    Optional<Settlement> findByStoreIdAndSettlementMonth(Long storeId, YearMonth settlementMonth);

    boolean existsByStoreIdAndSettlementMonthAndSettlementStatus(Long storeId, YearMonth settlementMonth, SettlementStatus settlementStatus);
}
