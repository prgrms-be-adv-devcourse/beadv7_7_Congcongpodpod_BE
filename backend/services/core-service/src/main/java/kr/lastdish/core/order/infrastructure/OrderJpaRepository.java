package kr.lastdish.core.order.infrastructure;

import kr.lastdish.core.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderJpaRepository extends JpaRepository<Order, Long> {
}
