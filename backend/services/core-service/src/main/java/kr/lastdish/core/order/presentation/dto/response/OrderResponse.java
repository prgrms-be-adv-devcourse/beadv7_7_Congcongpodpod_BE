package kr.lastdish.core.order.presentation.dto.response;

import java.math.BigDecimal;
import java.time.LocalTime;
import kr.lastdish.core.order.application.dto.OrderResult;
import kr.lastdish.core.order.domain.OrderStatus;
import kr.lastdish.core.order.domain.PaymentStatus;

public record OrderResponse(
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
    BigDecimal savedAmount,
    LocalTime pickupStartAt,
    LocalTime pickupEndAt) {

  public static OrderResponse from(OrderResult result) {
    return new OrderResponse(
        result.orderId(),
        result.memberId(),
        result.storeId(),
        result.status(),
        result.rejectReason(),
        result.paymentStatus(),
        result.memberName(),
        result.phone(),
        result.dishId(),
        result.dishName(),
        result.quantity(),
        result.unitPrice(),
        result.totalPrice(),
        result.savedAmount(),
        result.pickupStartAt(),
        result.pickupEndAt());
  }
}
