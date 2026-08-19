package kr.lastdish.core.store.presentation.dto;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import kr.lastdish.core.store.application.dto.StoreResult;
import kr.lastdish.core.store.domain.Category;
import kr.lastdish.core.store.domain.StoreStatus;

public record InternalStoreResponse(
    Long storeId,
    Long memberId,
    String storeName,
    String storeAddress,
    LocalTime openTime,
    LocalTime closeTime,
    StoreStatus status,
    BigDecimal latitude,
    BigDecimal longitude,
    Category category,
    List<DayOfWeek> holidays,
    InternalDishResponse dish) {

  public static InternalStoreResponse from(StoreResult store, InternalDishResponse dish) {
    return new InternalStoreResponse(
        store.storeId(),
        store.memberId(),
        store.storeName(),
        store.storeAddress(),
        store.openTime(),
        store.closeTime(),
        store.status(),
        store.latitude(),
        store.longitude(),
        store.category(),
        store.holidays(),
        dish);
  }
}
