package kr.lastdish.core.favorite.application.dto;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

public record FavoriteStoreResult(
    Long storeId,
    Long memberId,
    String storeName,
    String businessNumber,
    String storeAddress,
    String storePhone,
    LocalTime openTime,
    LocalTime closeTime,
    String status,
    BigDecimal latitude,
    BigDecimal longitude,
    String category,
    List<DayOfWeek> holidays,
    List<FavoriteDishResult> dishes) {}
