package kr.lastdish.core.order.infrastructure;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import kr.lastdish.core.order.domain.Order;
import kr.lastdish.core.order.domain.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderJpaRepository extends JpaRepository<Order, Long> {
  Optional<Order> findByIdAndIsDeletedFalse(Long orderId);

  @Query(
      """
      select count(o) > 0
      from Order o
      where o.storeId = :storeId
        and o.pickupCode = :pickupCode
        and o.isDeleted = false
        and o.status in ("RESERVED","PICKUP_READY")
      """)
  boolean existsActivePickupCode(
      @Param("storeId") Long storeId, @Param("pickupCode") String pickupCode);
    
  @Query(
      """
        SELECT o
        FROM Order o
        WHERE o.storeId = :storeId
          AND o.status IN :orderStatuses
          AND o.updatedAt >= :periodStart
          AND o.updatedAt < :periodEnd
        ORDER BY o.updatedAt ASC, o.id ASC
        """)
  List<Order> findSettlementTargetOrders(
      @Param("storeId") Long storeId,
      @Param("orderStatuses") List<OrderStatus> orderStatuses,
      @Param("periodStart") LocalDateTime periodStart,
      @Param("periodEnd") LocalDateTime periodEnd);
}
}
