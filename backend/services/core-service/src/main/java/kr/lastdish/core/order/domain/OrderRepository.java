package kr.lastdish.core.order.domain;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository {
  Order save(Order order);

  Order findByIdAndIsDeletedFalse(Long orderId);

  List<Order> findSettlementTargetOrders(
      Long storeId,
      List<OrderStatus> orderStatuses,
      LocalDateTime periodStart,
      LocalDateTime periodEnd);

  boolean validateActivePickUpCode(Long storeId, String pickUpCode);
}
