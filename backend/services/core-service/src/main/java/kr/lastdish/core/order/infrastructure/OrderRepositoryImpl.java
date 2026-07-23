package kr.lastdish.core.order.infrastructure;

import java.time.LocalDateTime;
import java.util.List;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.core.common.exception.ErrorCode;
import kr.lastdish.core.order.domain.Order;
import kr.lastdish.core.order.domain.OrderRepository;
import kr.lastdish.core.order.domain.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

  @Override
  public Order findPickupAvailableOrder(Long orderId, Long memberId) {
    return orderJpaRepository
        .findPickupAvailableOrder(orderId, memberId)
        .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
  }

  @Override
  public Page<Order> findAllByMemberIdAndStatus(
      Long memberId, OrderStatus status, Pageable pageable) {
    return orderJpaRepository.findAllByMemberIdAndStatus(memberId, status, pageable);
  }

  @Override
  public Page<Order> findAllByStoreIdAndStatus(
      Long storeId, OrderStatus status, Pageable pageable) {
    return orderJpaRepository.findAllByStoreIdAndStatus(storeId, status, pageable);
  }
}
