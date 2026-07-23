package kr.lastdish.core.cart.application.event;

public record DishStateChangedPayload(Long dishId, boolean available, Long stockQuantity) {}
