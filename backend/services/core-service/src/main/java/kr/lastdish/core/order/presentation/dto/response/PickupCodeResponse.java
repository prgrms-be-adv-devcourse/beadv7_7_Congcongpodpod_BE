package kr.lastdish.core.order.presentation.dto.response;

import java.time.LocalTime;
import kr.lastdish.core.order.application.dto.PickupCodeResult;

public record PickupCodeResponse(
    Long orderId,
    String dishName,
    String pickupCode,
    LocalTime pickupStartAt,
    LocalTime pickupEndAt) {
  public static PickupCodeResponse from(PickupCodeResult result) {
    return new PickupCodeResponse(
        result.orderId(),
        result.dishName(),
        result.pickupCode(),
        result.pickupStartAt(),
        result.pickupEndAt());
  }
}
