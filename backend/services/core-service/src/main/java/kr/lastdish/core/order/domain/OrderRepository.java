package kr.lastdish.core.order.domain;

public interface OrderRepository {
  Order save(Order order);

  Order findByIdAndIsDeletedFalse(Long orderId);

  boolean validateActivePickUpCode(Long storeId, String pickUpCode);
}
