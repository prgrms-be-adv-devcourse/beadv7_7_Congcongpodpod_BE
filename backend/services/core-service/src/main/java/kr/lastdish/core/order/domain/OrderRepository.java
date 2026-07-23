package kr.lastdish.core.order.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderRepository {
  Order save(Order order);

  Order findByIdAndIsDeletedFalse(Long orderId);

  boolean validateActivePickUpCode(Long storeId, String pickUpCode);

  Order findPickupAvailableOrder(Long orderId, Long memberId);

  Page<Order> findAllByMemberIdAndStatus(Long memberId, OrderStatus status, Pageable pageable);

  Page<Order> findAllByStoreIdAndStatus(Long storeId, OrderStatus status, Pageable pageable);
}
