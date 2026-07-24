package kr.lastdish.core.order.presentation.dto;

import java.time.LocalTime;
import kr.lastdish.core.order.domain.Order;

public record PickupCodeResponse(
    Long orderId,
    String dishName,
    String pickupCode,
    LocalTime pickupStartAt,
    LocalTime pickupEndAt) {
  public static PickupCodeResponse from(Order order) {
    return new PickupCodeResponse(
        order.getId(),
        order.getDishName(),
        order.getPickupCode(),
        order.getPickupStartAt(),
        order.getPickupEndAt());
  }
}
