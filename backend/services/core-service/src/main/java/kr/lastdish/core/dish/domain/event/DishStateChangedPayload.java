package kr.lastdish.core.dish.domain.event;

public record DishStateChangedPayload(boolean available, Long stockQuantity) {}
