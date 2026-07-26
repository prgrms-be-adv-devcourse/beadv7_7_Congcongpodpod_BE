package kr.lastdish.core.order.presentation.dto;

import java.math.BigDecimal;
import java.time.LocalTime;
import kr.lastdish.core.order.domain.Order;
import kr.lastdish.core.order.domain.OrderStatus;
import kr.lastdish.core.order.domain.PaymentStatus;

public record OrderResponse(
    Long orderId,
    Long memberId,
    Long storeId,
    OrderStatus status,
    String rejectReason,
    PaymentStatus paymentStatus,
    // String memberName,
    String phone,
    Long dishId,
    String dishName,
    Long quantity,
    BigDecimal unitPrice,
    BigDecimal totalPrice,
    LocalTime pickupStartAt,
    LocalTime pickupEndAt) {
  public static OrderResponse from(Order order) {
    return new OrderResponse(
        order.getId(),
        order.getMemberId(),
        order.getStoreId(),
        order.getStatus(),
        order.getRejectReason() == null ? null : order.getRejectReason().getMessage(),
        order.getPaymentStatus(),
        // order.getMemberName(),
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
