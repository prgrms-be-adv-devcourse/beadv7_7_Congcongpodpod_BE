package kr.lastdish.core.store.application.dto;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import kr.lastdish.core.dish.application.dto.InternalDishResult;
import kr.lastdish.core.store.domain.Category;
import kr.lastdish.core.store.domain.StoreStatus;

public record InternalStoreResult(
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
    InternalDishResult dish) {

  public static InternalStoreResult from(StoreResult store, InternalDishResult dish) {
    return new InternalStoreResult(
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
