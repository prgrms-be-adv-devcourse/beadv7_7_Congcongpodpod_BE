package kr.lastdish.core.dish.domain.event;

import java.math.BigDecimal;

/**
 * Dish 가격 변경 payload입니다.
 *
 * <p>정가(dishPrice)도 함께 싣는 이유: Cart가 절약 금액 산출을 위해 정가를 들고 있어서, 판매가만 보내면 정가만 바뀐 경우 Cart가 낡은 정가를 계속 들고
 * 있게 된다.
 */
public record DishPriceChangedPayload(BigDecimal dishPrice, BigDecimal unitPrice) {}
