package kr.lastdish.core.settlement.infrastructure;

import java.util.Collection;
import java.util.List;
import kr.lastdish.core.settlement.domain.SettlementDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaSettlementDetailRepository extends JpaRepository<SettlementDetail, Long> {
  boolean existsByOrderId(Long orderId);

  @Query(
      """
            SELECT detail.orderId
            FROM SettlementDetail detail
            WHERE detail.orderId IN :orderIds
            """)
  List<Long> findSettledOrderIds(@Param("orderIds") Collection<Long> orderIds);
}
