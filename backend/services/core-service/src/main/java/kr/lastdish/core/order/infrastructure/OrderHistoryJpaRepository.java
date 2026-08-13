package kr.lastdish.core.order.infrastructure;

import kr.lastdish.core.order.domain.OrderHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderHistoryJpaRepository extends JpaRepository<OrderHistory, Long> {}
