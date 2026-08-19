package kr.lastdish.core.store.domain.event;

import java.math.BigDecimal;
import java.time.LocalTime;
import kr.lastdish.core.store.domain.Category;

public record StoreChangedPayload(
        Long storeId,
    String storeName,
    String storeAddress,
    String storePhone,
    LocalTime openTime,
    LocalTime closeTime,
    BigDecimal latitude,
    BigDecimal longitude,
    Category category) {}
