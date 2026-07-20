package kr.lastdish.core.store.presentation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import kr.lastdish.core.store.application.dto.StoreResult;
import kr.lastdish.core.store.domain.StoreStatus;

public record StoreResponse(
    Long storeId,
    Long memberId,
    String storeName,
    String businessNumber,
    String storeAddress,
    String storePhone,
    @JsonFormat(pattern = "HH:mm") LocalTime openTime,
    @JsonFormat(pattern = "HH:mm") LocalTime closeTime,
    StoreStatus status,
    BigDecimal latitude,
    BigDecimal longitude,
    List<DayOfWeek> holidays) {

  public static StoreResponse from(StoreResult result) {
    return new StoreResponse(
        result.storeId(),
        result.memberId(),
        result.storeName(),
        result.businessNumber(),
        result.storeAddress(),
        result.storePhone(),
        result.openTime(),
        result.closeTime(),
        result.status(),
        result.latitude(),
        result.longitude(),
        result.holidays());
  }
}
