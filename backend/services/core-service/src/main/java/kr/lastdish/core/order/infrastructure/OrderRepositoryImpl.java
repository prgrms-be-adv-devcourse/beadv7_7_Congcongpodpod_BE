package kr.lastdish.core.order.infrastructure;

import java.time.LocalDateTime;
import java.util.List;
import kr.lastdish.core.common.exception.BusinessException;
import kr.lastdish.core.common.exception.ErrorCode;
import kr.lastdish.core.order.domain.Order;
import kr.lastdish.core.order.domain.OrderRepository;
import kr.lastdish.core.order.domain.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {

  private final OrderJpaRepository orderJpaRepository;

  @Override
  public Order save(Order order) {
    return orderJpaRepository.save(order);
  }

  @Override
  public Order findByIdAndIsDeletedFalse(Long orderId) {
    return orderJpaRepository
        .findByIdAndIsDeletedFalse(orderId)
        .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
  }

  @Override
  public List<Order> findSettlementTargetOrders(
      Long storeId,
      List<OrderStatus> orderStatuses,
      LocalDateTime periodStart,
      LocalDateTime periodEnd) {
    return orderJpaRepository.findSettlementTargetOrders(
        storeId, orderStatuses, periodStart, periodEnd);
  }

  public boolean validateActivePickUpCode(Long storeId, String pickUpCode) {
    return orderJpaRepository.existsActivePickupCode(storeId, pickUpCode);
  }
}
