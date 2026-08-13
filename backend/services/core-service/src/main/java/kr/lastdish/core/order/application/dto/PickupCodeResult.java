package kr.lastdish.core.order.application.dto;

import java.time.LocalTime;
import kr.lastdish.core.order.domain.Order;

public record PickupCodeResult(
    Long orderId,
    String dishName,
    String pickupCode,
    LocalTime pickupStartAt,
    LocalTime pickupEndAt) {
  public static PickupCodeResult from(Order order) {
    return new PickupCodeResult(
        order.getId(),
        order.getDishName(),
        order.getPickupCode(),
        order.getPickupStartAt(),
        order.getPickupEndAt());
  }
}
