package kr.lastdish.ai.elastic.presentation.dto;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

public record TestStoreIndexRequest(
    Long storeId,
    String storeName,
    String storeAddress,
    LocalTime openTime,
    LocalTime closeTime,
    String status,
    Double latitude,
    Double longitude,
    String category,
    List<DishRequest> dishes) {
  public record DishRequest(
      Long dishId,
      String dishName,
      String description,
      String category,
      String thumbnailUrl,
      Long stockQuantity,
      String dishStatus,
      BigDecimal dishPrice,
      BigDecimal discountPrice,
      LocalTime pickupStartTime,
      LocalTime pickupEndTime) {}
}
