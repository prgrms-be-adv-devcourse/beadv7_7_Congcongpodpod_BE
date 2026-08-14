package kr.lastdish.core.settlement.infrastructure;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import kr.lastdish.core.settlement.domain.Settlement;
import kr.lastdish.core.settlement.domain.SettlementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaSettlementRepository extends JpaRepository<Settlement, Long> {
  boolean existsByStoreIdAndSettlementMonth(Long storeId, YearMonth settlementMonth);

  Optional<Settlement> findByStoreIdAndSettlementMonth(Long storeId, YearMonth settlementMonth);

  boolean existsByStoreIdAndSettlementMonthAndSettlementStatus(
      Long storeId, YearMonth settlementMonth, SettlementStatus settlementStatus);

  Page<Settlement> findAllByStoreIdOrderBySettlementMonthDesc(Long storeId, Pageable pageable);

  Optional<Settlement> findByIdAndStoreId(Long settlementId, Long storeId);

  @Query(
      """
                SELECT s.storeId
                FROM Settlement s
                WHERE s.storeId IN :storeIds
                """)
  List<Long> findSettledStoreIds(@Param("storeIds") List<Long> storeIds);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
          value = """
                  TRUNCATE TABLE
                    settlement_details, settlements
                  RESTART INDENTITY
                  """,
          nativeQuery = true
  )
  void truncateAllSettlementData();
}
