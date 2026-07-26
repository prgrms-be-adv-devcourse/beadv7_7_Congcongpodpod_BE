package kr.lastdish.core.order.domain;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderRepository {
  Order save(Order order);

  Order findByIdAndIsDeletedFalse(Long orderId);

  Order findWithLockByIdAndIsDeletedFalse(Long orderId);

  List<Order> findSettlementTargetOrders(
      Long storeId,
      List<OrderStatus> orderStatuses,
      LocalDateTime periodStart,
      LocalDateTime periodEnd);

  boolean validateActivePickUpCode(Long storeId, String pickUpCode);

  Order findPickupAvailableOrder(Long orderId, Long memberId);

  Page<Order> findAllByMemberIdAndStatus(Long memberId, OrderStatus status, Pageable pageable);

  Page<Order> findAllByStoreIdAndStatus(Long storeId, OrderStatus status, Pageable pageable);
}
