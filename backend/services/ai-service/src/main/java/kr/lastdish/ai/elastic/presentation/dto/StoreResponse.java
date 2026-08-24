package kr.lastdish.ai.elastic.presentation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import kr.lastdish.ai.elastic.domain.document.StoreDocument;

public record StoreResponse(
    Long storeId,
    Long memberId,
    String storeName,
    String storeAddress,
    String storeDetailAddress,
    LocalTime openTime,
    LocalTime closeTime,
    LocalDateTime nextClosingAt,
    String status,
    BigDecimal latitude,
    BigDecimal longitude,
    String category,

    // 가장 싼 상품 정보만
    CheapestDishResponse cheapestDish) {
  public record CheapestDishResponse(
      Long dishId,
      Long storeId,
      String dishName,
      LocalDateTime registeredAt,
      String description,
      String thumbnailUrl,
      String dishStatus,
      BigDecimal dishPrice,
      BigDecimal discountPrice,
      LocalTime pickupStartTime,
      LocalTime pickupEndTime) {}

  public static StoreResponse from(StoreDocument doc) {
    CheapestDishResponse cheapestDish = null;

    if (doc.getDishes() != null && !doc.getDishes().isEmpty()) {
      // 가장 싼 상품 선별
      StoreDocument.DishItem minDish =
          doc.getDishes().stream()
              .filter(d -> d.getDiscountPrice() != null)
              .min(Comparator.comparing(StoreDocument.DishItem::getDiscountPrice))
              .orElse(doc.getDishes().get(0));

      cheapestDish =
          new CheapestDishResponse(
              minDish.getDishId(),
              doc.getStoreId(),
              minDish.getDishName(),
              null, // ES Document 미존재 필드
              minDish.getDescription(),
              minDish.getThumbnailUrl(),
              minDish.getDishStatus(),
              minDish.getDishPrice(),
              minDish.getDiscountPrice(),
              minDish.getPickupStartTime(),
              minDish.getPickupEndTime());
    }

    return new StoreResponse(
        doc.getStoreId(),
        null, // ES Document 미존재 필드
        doc.getStoreName(),
        doc.getStoreAddress(),
        null, // ES Document 미존재 필드
        doc.getOpenTime(),
        doc.getCloseTime(),
        null, // ES Document 미존재 필드
        doc.getStatus(),
        doc.getLocation() != null ? BigDecimal.valueOf(doc.getLocation().getLat()) : null,
        doc.getLocation() != null ? BigDecimal.valueOf(doc.getLocation().getLon()) : null,
        doc.getCategory(),
        cheapestDish);
  }
}
