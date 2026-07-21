package kr.lastdish.core.order.presentation.dto;

import java.math.BigDecimal;
import java.time.LocalTime;

public record OrderCreateRequest(
    Long storeId,
    Long dishId,
    // String memberName, // 추후 이름 추가
    String phone,
    String dishName,
    Long quantity,
    BigDecimal unitPrice,
    LocalTime pickupStartAt,
    LocalTime pickupEndAt) {}
