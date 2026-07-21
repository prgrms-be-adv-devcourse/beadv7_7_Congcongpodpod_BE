package kr.lastdish.core.order.presentation.dto;

import kr.lastdish.core.order.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalTime;

public record OrderCreateRequest(
        Long memberId,
        Long storeId,
        Long dishId,
        PaymentStatus paymentStatus,
        String memberName,
        String phone,
        String dishName,
        Long quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice,
        LocalTime pickupStartAt,
        LocalTime pickupEndAt
) {
}
