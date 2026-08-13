package kr.lastdish.core.order.infrastructure;

import kr.lastdish.core.order.domain.OrderHistory;
import kr.lastdish.core.order.domain.OrderHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OrderHistoryRepositoryImpl implements OrderHistoryRepository {

  private final OrderHistoryJpaRepository orderHistoryJpaRepository;

  @Override
  public OrderHistory save(OrderHistory orderHistory) {
    return orderHistoryJpaRepository.save(orderHistory);
  }
}
