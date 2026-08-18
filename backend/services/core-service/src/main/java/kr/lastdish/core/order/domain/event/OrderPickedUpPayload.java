package kr.lastdish.core.order.domain.event;

import java.math.BigDecimal;

/**
 * 정산과 포인트에서 함께 사용하는 픽업 완료 payload입니다. 정산 : storeId, finalOrderAmount 포인트 : memberId,
 * finalOrderAmount, savedAmount
 */
public record OrderPickedUpPayload(
    Long memberId, Long storeId, BigDecimal finalOrderAmount, BigDecimal savedAmount) {}
