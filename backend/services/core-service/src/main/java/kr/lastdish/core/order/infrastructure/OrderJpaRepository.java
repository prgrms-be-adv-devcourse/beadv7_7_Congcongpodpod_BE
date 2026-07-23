package kr.lastdish.core.order.infrastructure;

import java.util.Optional;
import kr.lastdish.core.order.domain.Order;
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
}
