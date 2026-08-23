package kr.lastdish.core.settlement.infrastructure;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
          SELECT DISTINCT s.storeId
          FROM Settlement s
          WHERE s.storeId IN :storeIds
            AND s.settlementMonth = :settlementMonth
            AND s.settlementStatus = :settlementStatus
          """)
  List<Long> findSettledStoreIds(
      @Param("storeIds") List<Long> storeIds,
      @Param("settlementMonth") YearMonth settlementMonth,
      @Param("settlementStatus") SettlementStatus settlementStatus);

  @Modifying
  @Query(value = """
        INSERT INTO settlements (
            store_id,
            settlement_month,
            period_start,
            period_end,
            total_order_count,
            gross_amount,
            fee_rate,
            fee_amount,
            settlement_amount,
            settlement_status,
            created_at,
            updated_at
        )
        VALUES (
            :storeId,
            :settlementMonth,
            :periodStart,
            :periodEnd,
            0,
            0,
            :feeRate,
            0,
            0,
            'ACCUMULATING',
            CURRENT_TIMESTAMP,
            CURRENT_TIMESTAMP
        )
        ON CONFLICT (store_id, settlement_month)
        DO NOTHING
        """,
          nativeQuery = true)
  void insertAccumulatingIfAbsent(
          @Param("storeId") Long storeId,
          @Param("settlementMonth") String settlementMonth,
          @Param("periodStart") LocalDateTime periodStart,
          @Param("periodEnd") LocalDateTime periodEnd,
          @Param("feeRate") BigDecimal feeRate
  );

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      value =
          """
                  TRUNCATE TABLE
                    settlement_details, settlements
                  RESTART IDENTITY
                  """,
      nativeQuery = true)
  void truncateAllSettlementData();
}
