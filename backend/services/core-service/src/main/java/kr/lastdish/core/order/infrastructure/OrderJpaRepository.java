package kr.lastdish.core.order.infrastructure;

import kr.lastdish.core.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderJpaRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByIdAndIsDeletedFalse(Long orderId);
}
