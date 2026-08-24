package kr.lastdish.core.store.application.dto;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
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
    String storeDetailAddress,
    LocalTime openTime,
    LocalTime closeTime,
    LocalDateTime nextClosingAt,
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
        store.storeDetailAddress(),
        store.openTime(),
        store.closeTime(),
        store.nextClosingAt(),
        store.status(),
        store.latitude(),
        store.longitude(),
        store.category(),
        store.holidays(),
        dish);
  }
}
