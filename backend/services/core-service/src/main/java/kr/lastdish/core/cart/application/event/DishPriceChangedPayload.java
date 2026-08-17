package kr.lastdish.core.cart.application.event;

import java.math.BigDecimal;

/**
 * Dish 가격 변경 이벤트에서 Cart가 사용하는 업무 데이터입니다.
 *
 * <p>Dish 식별자와 이벤트 버전은 EventMessage에서 가져옵니다.
 */
public record DishPriceChangedPayload(BigDecimal dishPrice, BigDecimal unitPrice) {}
