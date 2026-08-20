package kr.lastdish.core.order.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** 주문 화면에서 조회한 Dish 가격 버전을 전달하는 요청입니다. */
public record OrderCreateRequest(@NotNull @PositiveOrZero Long dishPriceVersion) {}
