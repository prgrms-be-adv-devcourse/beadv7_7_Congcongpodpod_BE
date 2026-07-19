package kr.lastdish.core.store.application.dto;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

public record RegisterStoreCommand(
        Long memberId,
        String storeName,
        String businessNumber,
        String storeAddress,
        String storePhone,
        LocalTime openTime,
        LocalTime closeTime,
        BigDecimal latitude,
        BigDecimal longitude,
        List<DayOfWeek> holidays
) {
}