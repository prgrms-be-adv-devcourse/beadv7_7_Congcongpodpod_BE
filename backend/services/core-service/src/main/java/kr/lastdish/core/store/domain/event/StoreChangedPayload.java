package kr.lastdish.core.store.domain.event;

import kr.lastdish.core.dish.domain.Dish;
import kr.lastdish.core.store.domain.Category;
import kr.lastdish.core.store.domain.StoreHoliday;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

public record StoreChangedPayload(
        String storeName,
        String storeAddress,
        String storePhone,
        LocalTime openTime,
        LocalTime closeTime,
        BigDecimal latitude,
        BigDecimal longitude,
        Category category,
        List<StoreHoliday> holidays) {
}
