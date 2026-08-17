package kr.lastdish.core.dish.domain.event;

import kr.lastdish.core.dish.domain.Dish;

public record DishCreatedPayload(Dish dish) {}
