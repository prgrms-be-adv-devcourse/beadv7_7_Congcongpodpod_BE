package kr.lastdish.core.order.application.dto;

import java.math.BigDecimal;
import java.time.LocalTime;
import kr.lastdish.core.order.domain.Order;
import kr.lastdish.core.order.domain.OrderStatus;
import kr.lastdish.core.order.domain.PaymentStatus;

public record OrderResult(
    Long orderId,
    Long memberId,
    Long storeId,
    OrderStatus status,
    String rejectReason,
    PaymentStatus paymentStatus,
    String memberName,
    String phone,
    Long dishId,
    String dishName,
    Long quantity,
    BigDecimal unitPrice,
    BigDecimal totalPrice,
    LocalTime pickupStartAt,
    LocalTime pickupEndAt) {
  public static OrderResult from(Order order) {
    return new OrderResult(
        order.getId(),
        order.getMemberId(),
        order.getStoreId(),
        order.getStatus(),
        order.getRejectReason() == null ? null : order.getRejectReason().getMessage(),
        order.getPaymentStatus(),
        order.getMemberName(),
        order.getPhone(),
        order.getDishId(),
        order.getDishName(),
        order.getQuantity(),
        order.getUnitPrice(),
        order.getTotalPrice(),
        order.getPickupStartAt(),
        order.getPickupEndAt());
  }
}
