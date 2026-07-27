package kr.lastdish.core.store.presentation.dto;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import kr.lastdish.core.store.application.dto.NearbyStoreResult;
import kr.lastdish.core.store.domain.Category;
import kr.lastdish.core.store.domain.StoreStatus;

public record NearbyStoreResponse(
    Long storeId,
    Long memberId,
    String storeName,
    String businessNumber,
    String storeAddress,
    String storePhone,
    LocalTime openTime,
    LocalTime closeTime,
    StoreStatus status,
    BigDecimal latitude,
    BigDecimal longitude,
    Category category,
    List<DayOfWeek> holidays,
    List<StoreDishResponse> dishes) {

  public static NearbyStoreResponse from(NearbyStoreResult result) {
    var store = result.store();
    return new NearbyStoreResponse(
        store.storeId(),
        store.memberId(),
        store.storeName(),
        store.businessNumber(),
        store.storeAddress(),
        store.storePhone(),
        store.openTime(),
        store.closeTime(),
        store.status(),
        store.latitude(),
        store.longitude(),
        store.category(),
        store.holidays(),
        result.dishes().stream().map(StoreDishResponse::from).toList());
  }
}
