package kr.lastdish.core.store.application.dto;

import kr.lastdish.core.store.domain.Store;
import kr.lastdish.core.store.domain.StoreHoliday;
import kr.lastdish.core.store.domain.StoreStatus;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

public record StoreResult(
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
        List<DayOfWeek> holidays
) {

    public static StoreResult from(Store store) {
        return new StoreResult(
                store.getId(),
                store.getMemberId(),
                store.getStoreName(),
                store.getBusinessNumber(),
                store.getStoreAddress(),
                store.getStorePhone(),
                store.getOpenTime(),
                store.getCloseTime(),
                store.getStatus(),
                store.getLatitude(),
                store.getLongitude(),
                store.getHolidays().stream()
                        .map(StoreHoliday::getDayOfWeek)
                        .toList()
        );
    }
}