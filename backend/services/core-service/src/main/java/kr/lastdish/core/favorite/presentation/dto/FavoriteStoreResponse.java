package kr.lastdish.core.favorite.presentation.dto;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import kr.lastdish.core.favorite.application.dto.FavoriteStoreResult;

public record FavoriteStoreResponse(
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
    List<FavoriteDishResponse> dishes) {

  public static FavoriteStoreResponse from(FavoriteStoreResult result) {
    return new FavoriteStoreResponse(
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
        result.category(),
        result.holidays(),
        result.dishes().stream().map(FavoriteDishResponse::from).toList());
  }
}
