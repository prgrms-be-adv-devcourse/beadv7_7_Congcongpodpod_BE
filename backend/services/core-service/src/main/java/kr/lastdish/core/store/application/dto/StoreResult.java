package kr.lastdish.core.store.application.dto;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import kr.lastdish.core.store.domain.Category;
import kr.lastdish.core.store.domain.Store;
import kr.lastdish.core.store.domain.StoreHoliday;
import kr.lastdish.core.store.domain.StoreStatus;

public record StoreResult(
    Long storeId,
    Long memberId,
    String storeName,
    String businessNumber,
    String storeAddress,
    String storeDetailAddress,
    String storePhone,
    LocalTime openTime,
    LocalTime closeTime,
    LocalDateTime nextClosingAt,
    StoreStatus status,
    BigDecimal latitude,
    BigDecimal longitude,
    Category category,
    List<DayOfWeek> holidays) {

  public static StoreResult from(Store store) {
    return new StoreResult(
        store.getId(),
        store.getMemberId(),
        store.getStoreName(),
        store.getBusinessNumber(),
        store.getStoreAddress(),
        store.getStoreDetailAddress(),
        store.getStorePhone(),
        store.getOpenTime(),
        store.getCloseTime(),
        store.getNextClosingAt(),
        store.getStatus(),
        store.getLatitude(),
        store.getLongitude(),
        store.getCategory(),
        store.getHolidays().stream().map(StoreHoliday::getDayOfWeek).toList());
  }
}
