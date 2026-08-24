package kr.lastdish.ai.elastic.infrastructure.client.dto;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

public record InternalStoreResponse(
    Long storeId,
    String storeName,
    String storeAddress,
    LocalTime openTime,
    LocalTime closeTime,
    String status,
    BigDecimal latitude,
    BigDecimal longitude,
    String category,
    List<InternalDishResponse> dishes) {}
